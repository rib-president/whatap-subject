package com.whatap.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatap.common.dto.CreateResponseDto;
import com.whatap.common.dto.ListItemResponseDto;
import com.whatap.common.dto.SuccessResponseDto;
import com.whatap.common.entity.ProductInfo;
import com.whatap.common.event.*;
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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ProductService {

  private final ProductRepository repository;
  private final ProductInfoRepository queryRepository;

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

  @DistributedLock(key = "#p0")
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

//  @DistributedLock(key = "#p0")
  @KafkaListener(topics = "order-events", groupId = "product-group")
  public void handleOrderCreatedEvent(String message) throws JsonProcessingException {
    System.out.println("Received message: " + message);

    OrderCreatedEvent event = mapper.readValue(message, OrderCreatedEvent.class);

    for(OrderCreatedEvent.Item item : event.getItems()) {
      Product product = repository.findById(item.getProductId())
          .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

      // 재고가 부족할 경우
      if(product.getStock() < item.getQuantity()) {
        StockUpdatedEvent failedEvent = StockUpdatedEvent.builder()
            .orderId(event.getOrderId())
            .isSuccess(false)
            .build();

        String failedMessage = mapper.writeValueAsString(failedEvent);
        producer.create(failedMessage);

        return ;
      }

      // 재고 있음
      product.update(null, null, product.getStock() - item.getQuantity());
      repository.save(product);
    }

    // StockUpdateEvent 발행
    StockUpdatedEvent successEvent = StockUpdatedEvent.builder()
        .orderId(event.getOrderId())
        .isSuccess(true)
        .build();

    String successMessage = mapper.writeValueAsString(successEvent);
    producer.create(successMessage);
  }

  @KafkaListener(topics = "order-events", groupId = "product-group")
  public void handleOrderCancelledEvent(OrderCancelledEvent event) {
    for(EventItem.Item item : event.getItems()) {
      Product product = repository.findById(item.getProductId())
          .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

      // 재고복구
      product.update(null, null, product.getStock() + item.getQuantity());
      repository.save(product);
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
