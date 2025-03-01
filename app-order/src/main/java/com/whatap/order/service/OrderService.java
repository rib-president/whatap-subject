package com.whatap.order.service;

import com.whatap.common.dto.ListItemResponseDto;
import com.whatap.order.dto.GetOrderResponseDto;
import com.whatap.order.dto.GetOrdersRequestDto;
import com.whatap.order.dto.GetOrdersResponseDto;
import com.whatap.order.entity.Order;
import com.whatap.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OrderService {
  private final OrderRepository repository;

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
}
