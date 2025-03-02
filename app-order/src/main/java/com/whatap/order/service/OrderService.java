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
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OrderService {
  private final OrderRepository repository;
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
    Page<Order> orders = repository.findAllByCriteria(query, pageable);

    List<GetOrdersResponseDto> items = orders.stream()
        .map(order -> GetOrdersResponseDto.builder()
            .id(order.getId().toString())
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
    Order order = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("ORDER_NOT_FOUND"));

    return GetOrderResponseDto.builder()
        .id(order.getId().toString())
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

    Order order = Order.builder()
        .status(OrderStatus.PENDING)
        .build();

    BigDecimal totalPrice = BigDecimal.ZERO;
    List<OrderItem> orderItems = new ArrayList<>();
    List<OrderCreatedEvent.Item> eventItems = new ArrayList<>();
    for (OrderProductRequestDto.Item item : body.getItems()) {
      ProductInfo productInfo = queryRepository.findById(item.getProductId().toString())
          .orElse(null);

      if (productInfo == null) {
        String url = baseUrl + appProductName + setProductInfoApi.replace("{}", item.getProductId().toString());
        SuccessResponseDto resp = httpClient.post(url, null, SuccessResponseDto.class);

        productInfo = queryRepository.findById(item.getProductId().toString())
            .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));
      }

      OrderItem orderItem = OrderItem.builder()
          .order(order)
          .productId(item.getProductId())
          .productName(productInfo.getName())
          .productPrice(productInfo.getPrice())
          .quantity(item.getQuantity())
          .build();
      orderItems.add(orderItem);

      totalPrice = totalPrice.add(orderItem.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

      OrderCreatedEvent.Item eventItem = new EventItem.Item(orderItem.getProductId(), orderItem.getQuantity());
      eventItems.add(eventItem);
    }

    order.setOrderItems(orderItems);
    order.setTotalPrice(totalPrice);

    repository.save(order);

    OrderCreatedEvent event = new OrderCreatedEvent();
    event.setOrderId(order.getId());
    event.setItems(eventItems);

    producer.create(event);

    return CreateResponseDto.<String>builder()
        .id(order.getId().toString())
        .build();
  }

  public SuccessResponseDto changeOrder(BigInteger id, ChangeOrderRequestDto body) {
    Order order = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("ORDER_NOT_FOUND"));

    order.getOrderItems();

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }

  public SuccessResponseDto deleteOrder(BigInteger id) {
    Order order = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("ORDER_NOT_FOUND"));

    OrderCancelledEvent event = new OrderCancelledEvent();
    event.setOrderId(id);
    event.setItems(order.getOrderItems().stream()
        .map(item -> {
          EventItem.Item eventItem = new EventItem.Item();
          eventItem.setProductId(item.getProductId());
          eventItem.setQuantity(item.getQuantity());
          return eventItem;
        })
        .collect(Collectors.toList()));

    repository.delete(order);

    producer.create(event);

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }


  @KafkaListener(topics = "product-events", groupId = "order-group")
  public void handleStockUpdatedEvent(Event event) throws JsonProcessingException {
    log.info("Received message: {}", event);

    Order order = repository.findById(event.getOrderId())
        .orElseThrow(() -> new RuntimeException("ORDER_NOT_FOUND"));

    if (EventType.STOCK_UPDATED.equals(event.getEventType())) {
      StockUpdatedEvent stockUpdatedEvent = mapper.convertValue(event, StockUpdatedEvent.class);

      if (stockUpdatedEvent.getIsSuccess()) {
        // 주문 성공
        order.setStatus(OrderStatus.CONFIRMED);
      } else {
        // 주문 실패(재고부족)
        order.setStatus(OrderStatus.CANCELED);
      }

      repository.save(order);
    }

  }
}
