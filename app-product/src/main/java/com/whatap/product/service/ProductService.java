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
    Product product = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

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

    Page<Product> products = repository.findAllByCriteria(query, pageable);

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

    Product product = Product.builder()
        .name(body.getName())
        .stock(body.getStock())
        .price(body.getPrice())
        .build();

    repository.save(product);

    this.saveProductInfo(product);

    return CreateResponseDto.<String>builder()
        .id(product.getId().toString())
        .build();
  }

  @DistributedLock(key = "#id")
  public SuccessResponseDto updateProduct(BigInteger id, UpdateProductRequestDto body) {
    Product product = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

    product.update(body.getName(), body.getPrice(), body.getStock());

    repository.save(product);

    this.saveProductInfo(product);

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }

  public SuccessResponseDto deleteProduct(BigInteger id) {
    Product product = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

    repository.delete(product);

    queryRepository.deleteById(id.toString());

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }

  public SuccessResponseDto setProductInfo(BigInteger id) {

    Product product = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

    this.saveProductInfo(product);

    return SuccessResponseDto.builder()
        .success(true)
        .build();
  }

  @KafkaListener(topics = "order-events", groupId = "product-group")
  public void handleOrderEvent(Event event) throws JsonProcessingException {
    log.info("Received message: {}, {}", event.getEventType(), event.getOrderId());

    switch (event.getEventType()) {
      case ORDER_CREATED:
        OrderCreatedEvent orderCreatedEvent = mapper.convertValue(event, OrderCreatedEvent.class);
        this.handleOrderCreatedEvent(orderCreatedEvent);
        break;
      case ORDER_UPDATED:
        OrderUpdatedEvent orderUpdatedEvent = mapper.convertValue(event, OrderUpdatedEvent.class);
        this.handleOrderUpdatedEvent(orderUpdatedEvent);
        break;
      case ORDER_CANCELLED:
        OrderCancelledEvent orderCancelledEvent = mapper.convertValue(event, OrderCancelledEvent.class);
        this.handleOrderCancelledEvent(orderCancelledEvent);
        break;
    }
  }

  private void handleOrderCreatedEvent(OrderCreatedEvent event) throws JsonProcessingException {
    List<Pair<Product, Integer>> updatedProducts = new ArrayList<>();
    for (OrderCreatedEvent.Item item : event.getItems()) {

      Boolean isAvailable = lockService.updateStockWithLock(item);
      if (!isAvailable) {
        // StockUpdateEvent 발행
        StockFailedEvent failedEvent = new StockFailedEvent(event.getOrderId(), EventType.STOCK_FAILED);

        producer.create(failedEvent);
        return;
      } else {
        Product product = repository.findById(item.getProductId())
            .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

        Pair<Product, Integer> updatedProduct = Pair.of(product, product.getStock() - (item.getQuantity() - item.getLatestQuantity()));
        updatedProducts.add(updatedProduct);
      }
    }

    updatedProducts
            .forEach(updatedProduct -> updatedProduct.getFirst().setStock(updatedProduct.getSecond()));

    // StockUpdateEvent 발행
    StockUpdatedEvent successEvent = new StockUpdatedEvent();
    successEvent.setOrderId(event.getOrderId());

    producer.create(successEvent);
  }

  private void handleOrderUpdatedEvent(OrderUpdatedEvent event) throws JsonProcessingException {
    List<Pair<Product, Integer>> updatedProducts = new ArrayList<>();
    for (OrderUpdatedEvent.Item item : event.getItems()) {
      Boolean isAvailable = lockService.updateStockWithLock(item);
      if (!isAvailable) {
        // StockRollbackEvent 발행
        StockFailedEvent failedEvent = new StockFailedEvent(event.getOrderId(), EventType.STOCK_ROLLBACK, event.getItems());

        producer.create(failedEvent);
        return;
      } else {
        Product product = repository.findById(item.getProductId())
            .orElse(null);

        if(product != null) {
          Pair<Product, Integer> updatedProduct = Pair.of(product, product.getStock() - (item.getQuantity() - item.getLatestQuantity()));
          updatedProducts.add(updatedProduct);
        }
      }
    }

    updatedProducts
        .forEach(updatedProduct -> updatedProduct.getFirst().setStock(updatedProduct.getSecond()));

    // StockUpdateEvent 발행
    StockUpdatedEvent successEvent = new StockUpdatedEvent();
    successEvent.setOrderId(event.getOrderId());

    producer.create(successEvent);
  }

  private void handleOrderCancelledEvent(OrderCancelledEvent event) {
    for (EventItem.Item item : event.getItems()) {
      Product product = repository.findById(item.getProductId())
          .orElse(null);

      if(product != null) {
        // 재고복구
        product.update(null, null, product.getStock() + item.getQuantity());
        repository.save(product);
      }
    }
  }

  private void saveProductInfo(Product product) {
    // 공통 정보 저장
    ProductInfo productInfo = ProductInfo.builder()
        .id(product.getId().toString())
        .name(product.getName())
        .price(product.getPrice())
        .build();

    queryRepository.save(productInfo);
  }

}
