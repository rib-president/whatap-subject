package com.whatap.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatap.common.client.HttpClient;
import com.whatap.common.dto.CreateResponseDto;
import com.whatap.common.dto.ListItemResponseDto;
import com.whatap.common.dto.SuccessResponseDto;
import com.whatap.common.entity.ProductInfo;
import com.whatap.common.event.*;
import com.whatap.common.event.enums.EventType;
import com.whatap.common.repository.ProductInfoRepository;
import com.whatap.order.dto.*;
import com.whatap.order.entity.Order;
import com.whatap.order.entity.OrderItem;
import com.whatap.order.enums.OrderStatus;
import com.whatap.order.producer.OrderProducer;
import com.whatap.order.repository.OrderItemRepository;
import com.whatap.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OrderService {
  private final OrderRepository repository;
  private final OrderItemRepository itemRepository;
  private final ProductInfoRepository queryRepository;

  private final HttpClient httpClient;

  private final OrderProducer producer;
  private final ObjectMapper mapper;

  @Value("${service.base-url}")
  private String baseUrl;
  @Value("${service.app-product.name}")
  private String appProductName;
  @Value("${service.app-product.api.set-product-info}")
  private String setProductInfoApi;

  public ListItemResponseDto<GetOrdersResponseDto> getOrders(GetOrdersRequestDto query, Pageable pageable) {
    // queryDSL을 사용하여 조건에 맞는 order 리스트 검색
    Page<Order> orders = repository.findAllByCriteria(query, pageable);

    // 검색된 리스트를 response dto로 변환
    List<GetOrdersResponseDto> items = orders.stream()
        .map(order -> GetOrdersResponseDto.builder()
            .id(order.getId().toString())
            .ordererName(order.getOrdererName())
            .totalPrice(order.getTotalPrice().toString())
            .item(order.getOrderItems().stream().findAny().get()
                .getProductName() + (order.getOrderItems().size() > 1 ? " 외 " + (order.getOrderItems().size() - 1) + "건" : ""))
            .createdAt(order.getCreatedAt().toString())
            .updatedAt(order.getUpdatedAt().toString())
            .build())
        .collect(Collectors.toList());

    return ListItemResponseDto.<GetOrdersResponseDto>builder()
        .items(items)
        .total(orders.getTotalElements())
        .count(orders.getNumberOfElements())
        .limit(pageable.getPageSize())
        .offset(pageable.getOffset())
        .build();
  }

  public GetOrderResponseDto getOrder(BigInteger id) {
    // id를 소유한 order 가져오기
    Order order = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("ORDER_NOT_FOUND"));

    // 조회된 order를 response dto로 변환
    return GetOrderResponseDto.builder()
        .id(order.getId().toString())
        .status(order.getStatus())
        .ordererName(order.getOrdererName())
        .ordererPhoneNumber(order.getOrdererPhoneNumber())
        .ordererAddress(order.getOrdererAddress())
        .totalPrice(order.getTotalPrice().toString())
        .items(order.getOrderItems().stream()
            .map(item -> GetOrderResponseDto.Item.builder()
                .id(item.getId().toString())
                .productName(item.getProductName())
                .productPrice(item.getProductPrice().toString())
                .quantity(item.getQuantity())
                .build())
            .collect(Collectors.toList()))
        .createdAt(order.getCreatedAt().toString())
        .updatedAt(order.getUpdatedAt().toString())
        .build();
  }

  public CreateResponseDto<String> orderProduct(OrderProductRequestDto body) throws JsonProcessingException {

    // 재고 확인 전이므로 대기(PENDING) 상태의 order entity 생성
    Order order = Order.builder()
        .status(OrderStatus.PENDING)
        .ordererName(body.getOrdererName())
        .ordererPhoneNumber(body.getOrdererPhoneNumber())
        .ordererAddress(body.getOrdererAddress())
        .build();

    BigDecimal totalPrice = BigDecimal.ZERO;
    List<OrderItem> orderItems = new ArrayList<>();
    List<OrderCreatedEvent.Item> eventItems = new ArrayList<>();

    // 요청값에 있는 item을 반복문을 돌면서 order item entity 생성
    for (OrderItemDto item : body.getItems()) {
      OrderItem orderItem = this.createOrderItem(item);
      orderItem.setOrder(order);
      orderItems.add(orderItem);

      // order의 총 금액 더하기
      totalPrice = totalPrice.add(orderItem.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

      // 재고 확인을 위한 주문생성이벤트 아이템 객체
      OrderCreatedEvent.Item eventItem = new EventItem.Item(orderItem.getProductId(), orderItem.getQuantity(), 0);
      eventItems.add(eventItem);
    }

    order.setOrderItems(orderItems);
    order.setTotalPrice(totalPrice);

    // jpa repository에 주문 저장
    repository.save(order);

    // 재고 확인을 위한 주문생성이벤트 객체 생성
    OrderCreatedEvent event = new OrderCreatedEvent();
    event.setOrderId(order.getId());
    event.setItems(eventItems);

    // 이벤트 발행
    producer.create(event);

    return CreateResponseDto.<String>builder()
        .id(order.getId().toString())
        .build();
  }

  public SuccessResponseDto changeOrder(BigInteger id, ChangeOrderRequestDto body) {
    // id를 가진 order entity 가져오기
    Order order = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("ORDER_NOT_FOUND"));

    // 대기(PENDING), 취소(CANCELED) 상태인 주문은 수정 불가
    if (!OrderStatus.CONFIRMED.equals(order.getStatus())) {
      throw new RuntimeException("ORDER_REQUEST_FORBIDDEN");
    }

    // order entity에 연결된 order item 리스트
    List<OrderItem> orderItems = order.getOrderItems();

    List<EventItem.Item> eventItems = new ArrayList<>();
    List<OrderItem> newOrderItems = new ArrayList<>();
    BigDecimal totalPrice = BigDecimal.ZERO;

    // 요청값의 item을 반복문을 수행하며 처리
    for (ChangeOrderRequestDto.Item item : body.getItems()) {
      // 현재 order item 중 요청값의 item과 일치하는 order item 검색
      OrderItem orderItem = orderItems.stream()
          .filter(oi -> item.getProductId().equals(oi.getProductId()))
          .findAny()
          .orElse(null);

      // 최근 수량 - 롤백을 위함
      Integer latestQuantity = 0;

      if (orderItem == null) {
        // 새로 추가된 상품일 경우 - order item entity 생성
        orderItem = this.createOrderItem(item);
        orderItem.setOrder(order);

        latestQuantity = 0;
      } else {
        // 기존 주문된 상품일 경우 - 변경된 수량 반영
        latestQuantity = orderItem.getQuantity();
        orderItem.setQuantity(item.getQuantity());
      }

      // 변경된 order item을 업데이트된 order item 리스트에 담기
      newOrderItems.add(orderItem);

      // 상품 및 상품 수량 변경으로 order의 총 금액 재계산
      totalPrice = totalPrice.add(orderItem.getProductPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));

      // 재고 확인을 위한 주문수정이벤트 아이템 생성
      EventItem.Item eventItem = new EventItem.Item(orderItem.getProductId(), item.getQuantity(), latestQuantity);
      eventItems.add(eventItem);
    }

    /**
     * 기존에 있었으나 수정 요청에서 사라진 order item 리스트를 주문수정이벤트 아이템에 함께 담기
     * product service에서 재고원복을 하기 위함
     */
    List<EventItem.Item> rollbackItems = orderItems.stream()
        .filter(orderItem -> newOrderItems.stream()
            .noneMatch(newItem -> Objects.equals(newItem.getId(), orderItem.getId())))
        .map(orderItem -> {
          EventItem.Item rollbackedItem =  new EventItem.Item(orderItem.getProductId(), 0, orderItem.getQuantity());
          rollbackedItem.setProductName(orderItem.getProductName());
          rollbackedItem.setProductPrice(orderItem.getProductPrice());

          return rollbackedItem;
        })
        .collect(Collectors.toList());
    eventItems.addAll(rollbackItems);

    // 주문수정이벤트 생성
    OrderUpdatedEvent updatedEvent = new OrderUpdatedEvent(id, eventItems);

    // order에 변경된 값 적용
    order.update(OrderStatus.PENDING, totalPrice, body.getOrdererName(), body.getOrdererPhoneNumber(), body.getOrdererAddress());
    orderItems.clear();
    orderItems.addAll(newOrderItems);
    repository.saveAndFlush(order);

    // 주문수정이벤트 발행
    producer.create(updatedEvent);

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }

  public SuccessResponseDto deleteOrder(BigInteger id) {
    // id를 가진 order entity 가져오기
    Order order = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("ORDER_NOT_FOUND"));

    // 확정(CONFIRMED) 상태의 주문일 경우에만 주문취소이벤트 발행하여 재고 원복
    if (OrderStatus.CONFIRMED.equals(order.getStatus())) {
      OrderCancelledEvent event = new OrderCancelledEvent();
      event.setOrderId(id);
      event.setItems(order.getOrderItems().stream()
          .map(item -> new EventItem.Item(item.getProductId(), item.getQuantity(), 0))
          .collect(Collectors.toList()));

      producer.create(event);
    }

    // order entity 삭제
    repository.delete(order);

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }


  @KafkaListener(topics = "product-events", groupId = "order-group")
  public void handleStockUpdatedEvent(Event event) throws JsonProcessingException {
    log.info("Received message: {} {}", event.getOrderId(), event.getEventType());

    // id를 가진 order entity 가져오기
    Order order = repository.findById(event.getOrderId())
        .orElseThrow(() -> new RuntimeException("ORDER_NOT_FOUND"));

    if (EventType.STOCK_UPDATED.equals(event.getEventType())) {
      // 재고변경 성공일 경우 - 확정(CONFIRMED) 상태로 변경
      order.setStatus(OrderStatus.CONFIRMED);

    } else if (EventType.STOCK_FAILED.equals(event.getEventType())) {
      // 재고변경 실패일 경우 - 취소(CANCELED) 상태로 변경
      order.setStatus(OrderStatus.CANCELED);

    } else if (EventType.STOCK_ROLLBACK.equals(event.getEventType())) {
      // 재고원복해야할 경우 - 주문수정 API에서 주문 수량을 재고 이상으로 수정하였을 경우 이전 주문 데이터로 돌려놓아야 함

      StockFailedEvent stockFailedEvent = mapper.convertValue(event, StockFailedEvent.class);

      // 현재 order item 리스트
      List<OrderItem> orderItems = order.getOrderItems();
      BigDecimal totalPrice = BigDecimal.ZERO;

      // 이벤트 응답값의 item을 반복문을 돌며 처리
      for (EventItem.Item eventItem : stockFailedEvent.getItems()) {

        // 현재 order item 리스트 중에서 이벤트 응답값의 item과 동일한 상품의 order item 가져오기
        OrderItem foundItem = orderItems.stream().filter(item -> item.getProductId().equals(eventItem.getProductId()))
            .findAny()
            .orElse(null);

        // 현재 order item에 있는 경우: 주문수정 API에서 새로 추가 또는 유지되는 order item
        if (foundItem != null) {

          // 롤백을 위한 최근 수량이 0보다 큼 -> 기존에 존재하던 order item
          if (eventItem.getLatestQuantity() > 0) {
            // 최근 수량으로 원복
            foundItem.setQuantity(eventItem.getLatestQuantity());

            // 재고 부족으로 order item의 수량이 원복됨으로 order의 총 금액 재계산
            totalPrice = totalPrice.add(foundItem.getProductPrice().multiply(BigDecimal.valueOf(eventItem.getLatestQuantity())));

          } else {
            // 롤백을 위한 최근 수량이 0 -> 주문 수정 API에서 새로 추가된 order item으로 삭제
            orderItems.remove(foundItem);
          }
        } else {  // 현재 order item에 없음: 주문수정 API에서 삭제되었던 order item

          // 롤백을 위해 order item entity 다시 생성
          OrderItem rollbackedOrderItem = OrderItem.builder()
              .order(order)
              .productId(eventItem.getProductId())
              .productName(eventItem.getProductName())
              .productPrice(eventItem.getProductPrice())
              .quantity(eventItem.getLatestQuantity())
              .build();

          // 재생성되는 order item의 금액을 반영하기 위해 order의 총 금액 재계산
          totalPrice = totalPrice.add(eventItem.getProductPrice().multiply(BigDecimal.valueOf(eventItem.getLatestQuantity())));
          orderItems.add(rollbackedOrderItem);
        }
      }

      // order에 총금액 및 이전 상태(CONFIRMED) 적용
      order.setTotalPrice(totalPrice);
      order.setStatus(OrderStatus.CONFIRMED);

    }

    // order 수정
    repository.save(order);
  }

  private OrderItem createOrderItem(OrderItemDto item) {
    // product id로 redis에 있는 상품정보 가져오기
    ProductInfo productInfo = queryRepository.findById(item.getProductId().toString())
        .orElse(null);

    if (productInfo == null) {
      // 상품정보가 없을 경우 product service의 상품정보입력 REST API 호출: 응답 받은 후 처리해야하므로 RestTemplate 사용(동기방식)
      String url = baseUrl + appProductName + setProductInfoApi.replace("{}", item.getProductId().toString());
      SuccessResponseDto resp = httpClient.post(url, null, SuccessResponseDto.class);

      // redis repository에서 다시 한 번 product id로 상품 조회
      productInfo = queryRepository.findById(item.getProductId().toString())
          .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));
    }

    // order item entity 생성
    OrderItem orderItem = OrderItem.builder()
        .productId(item.getProductId())
        .productName(productInfo.getName())
        .productPrice(productInfo.getPrice())
        .quantity(item.getQuantity())
        .build();

    return orderItem;
  }
}
