package com.whatap.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatap.common.dto.CreateResponseDto;
import com.whatap.common.dto.ListItemResponseDto;
import com.whatap.common.dto.SuccessResponseDto;
import com.whatap.common.entity.ProductInfo;
import com.whatap.common.event.*;
import com.whatap.common.event.enums.EventType;
import com.whatap.common.repository.ProductInfoRepository;
import com.whatap.product.aop.annotation.DistributedLock;
import com.whatap.product.dto.*;
import com.whatap.product.entity.Product;
import com.whatap.product.producer.ProductProducer;
import com.whatap.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ProductService {

  private final ProductRepository repository;
  private final ProductInfoRepository queryRepository;
  private final ProductLockService lockService;

  private final ObjectMapper mapper;
  private final ProductProducer producer;

  public GetProductResponseDto getProduct(BigInteger id) {
    // id를 가진 product entity 가져오기
    Product product = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

    // product entity를 response dto로 변환
    return GetProductResponseDto.builder()
        .id(product.getId().toString())
        .name(product.getName())
        .price(product.getPrice().toString())
        .stock(product.getStock())
        .createdAt(product.getCreatedAt().toString())
        .updatedAt(product.getUpdatedAt().toString())
        .build();
  }

  public ListItemResponseDto<GetProductsByPaginationResponseDto> getProductsByPagination(GetProductsByPaginationRequestDto query, Pageable pageable) {
    // queryDSL을 사용하여 조건에 맞는 product 리스트 검색
    Page<Product> products = repository.findAllByCriteria(query, pageable);

    // 검색된 product 리스트를 response dto로 변환
    List<GetProductsByPaginationResponseDto> items = products.stream()
        .map(product -> GetProductsByPaginationResponseDto.builder()
            .id(product.getId().toString())
            .name(product.getName())
            .price(product.getPrice().toString())
            .stock(product.getStock())
            .createdAt(product.getCreatedAt().toString())
            .updatedAt(product.getUpdatedAt().toString())
            .build())
        .collect(Collectors.toList());

    return ListItemResponseDto.<GetProductsByPaginationResponseDto>builder()
        .items(items)
        .total(products.getTotalElements())
        .count(products.getNumberOfElements())
        .limit(pageable.getPageSize())
        .offset(pageable.getOffset())
        .build();
  }

  public CreateResponseDto<String> addProduct(AddProductRequestDto body) {
    // product entity 생성
    Product product = Product.builder()
        .name(body.getName())
        .stock(body.getStock())
        .price(body.getPrice())
        .build();

    // product 저장
    repository.save(product);

    // redis repository에 상품 정보 put - order service에서 사용되는 데이터
    this.saveProductInfo(product);

    return CreateResponseDto.<String>builder()
        .id(product.getId().toString())
        .build();
  }

  @DistributedLock(key = "#id") // redisson을 사용한 분산락 적용 어노테이션(상품의 id가 고유키로 사용됨)
  public SuccessResponseDto updateProduct(BigInteger id, UpdateProductRequestDto body) {
    // id를 가진 product entity 가져오기
    Product product = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

    // product 정보 변경
    product.update(body.getName(), body.getPrice(), body.getStock());
    repository.save(product);

    // redis repository에 변경된 상품 정보 put - order service에서 사용되는 데이터
    this.saveProductInfo(product);

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }

  public SuccessResponseDto deleteProduct(BigInteger id) {
    // id를 가진 product entity 가져오기
    Product product = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

    // 상품 삭제
    repository.delete(product);

    // redis repository에서도 상품 정보 삭제
    queryRepository.deleteById(id.toString());

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }

  /**
   * redis repository에 상품 정보(상품명, 상품금액)이 없을 경우 order service에서 요청되는 상품 정보 PUT API
   */
  public SuccessResponseDto setProductInfo(BigInteger id) {
    // id를 가진 product entity 가져오기
    Product product = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

    // redis repository에 상품 정보 put - order service에서 사용되는 데이터
    this.saveProductInfo(product);

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }

  /**
   * order producer에서 발행한 메세지를 수신하는 kafka listener
   */
  @KafkaListener(topics = "order-events", groupId = "product-group")
  public void handleOrderEvent(Event event) throws JsonProcessingException {
    log.info("Received message: {}, {}", event.getEventType(), event.getOrderId());

    switch (event.getEventType()) {
      case ORDER_CREATED: // 주문생성이벤트 - 재고 확인 및 차감
        OrderCreatedEvent orderCreatedEvent = mapper.convertValue(event, OrderCreatedEvent.class);
        this.handleOrderCreatedEvent(orderCreatedEvent);
        break;
      case ORDER_UPDATED: // 주문수정이벤트 - 재고 확인 및 차감/원복
        OrderUpdatedEvent orderUpdatedEvent = mapper.convertValue(event, OrderUpdatedEvent.class);
        this.handleOrderUpdatedEvent(orderUpdatedEvent);
        break;
      case ORDER_CANCELLED: // 주문취소이벤트 - 재고 원복
        OrderCancelledEvent orderCancelledEvent = mapper.convertValue(event, OrderCancelledEvent.class);
        this.handleOrderCancelledEvent(orderCancelledEvent);
        break;
    }
  }

  private void handleOrderCreatedEvent(OrderCreatedEvent event) throws JsonProcessingException {
    List<Pair<Product, Integer>> updatedProducts = new ArrayList<>();

    // 발행된 이벤트의 item을 반복문을 돌며 처리
    for (OrderCreatedEvent.Item item : event.getItems()) {

      Boolean isAvailable = lockService.updateStockWithLock(item);
      if (!isAvailable) {
        // 재고가 부족할 경우 - 재고수정실패이벤트 발행
        StockFailedEvent failedEvent = new StockFailedEvent(event.getOrderId(), EventType.STOCK_FAILED);
        producer.create(failedEvent);

        return;
      } else {
        // 재고가 있을 경우 - 반복문 밖에서 재고변경을 위한 데이터 처리
        Product product = repository.findById(item.getProductId())
            .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

        Pair<Product, Integer> updatedProduct = Pair.of(product, product.getStock() - (item.getQuantity() - item.getLatestQuantity()));
        updatedProducts.add(updatedProduct);
      }
    }

    // 재고 변경
    updatedProducts
            .forEach(updatedProduct -> updatedProduct.getFirst().setStock(updatedProduct.getSecond()));

    // 재고수정성공 이벤트 발행
    StockUpdatedEvent successEvent = new StockUpdatedEvent();
    successEvent.setOrderId(event.getOrderId());

    producer.create(successEvent);
  }

  private void handleOrderUpdatedEvent(OrderUpdatedEvent event) throws JsonProcessingException {
    List<Pair<Product, Integer>> updatedProducts = new ArrayList<>();

    // 발행된 이벤트의 item을 반복문을 돌며 처리
    for (OrderUpdatedEvent.Item item : event.getItems()) {
      Boolean isAvailable = lockService.updateStockWithLock(item);
      if (!isAvailable) {
        // 재고가 부족할 경우 - 재고롤백이벤트 발행
        StockFailedEvent failedEvent = new StockFailedEvent(event.getOrderId(), EventType.STOCK_ROLLBACK, event.getItems());
        producer.create(failedEvent);

        return;
      } else {
        // 재고가 있을 경우 - 반복문 밖에서 재고변경을 위한 데이터 처리
        Product product = repository.findById(item.getProductId())
            .orElse(null);

        if(product != null) {
          Pair<Product, Integer> updatedProduct = Pair.of(product, product.getStock() - (item.getQuantity() - item.getLatestQuantity()));
          updatedProducts.add(updatedProduct);
        }
      }
    }

    // 재고 변경
    updatedProducts
        .forEach(updatedProduct -> updatedProduct.getFirst().setStock(updatedProduct.getSecond()));

    // 재고수정성공 이벤트 발행
    StockUpdatedEvent successEvent = new StockUpdatedEvent();
    successEvent.setOrderId(event.getOrderId());

    producer.create(successEvent);
  }

  private void handleOrderCancelledEvent(OrderCancelledEvent event) {
    // 발행된 이벤트의 item을 반복문을 돌며 처리
    for (EventItem.Item item : event.getItems()) {
      // id를 가진 product entity 가져오기
      Product product = repository.findById(item.getProductId())
          .orElse(null);

      if(product != null) {
        // 취소된 주문의 order item: product가 삭제된 상품이 아니라면 재고 복구
        product.update(null, null, product.getStock() + item.getQuantity());
        repository.save(product);
      }
    }
  }

  private void saveProductInfo(Product product) {
    // 상품정보 entity 생성
    ProductInfo productInfo = ProductInfo.builder()
        .id(product.getId().toString())
        .name(product.getName())
        .price(product.getPrice())
        .build();

    // redis repository에 저장
    queryRepository.save(productInfo);
  }

}
