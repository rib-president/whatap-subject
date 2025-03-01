package com.whatap.order.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatap.common.dto.ListItemResponseDto;
import com.whatap.order.dto.GetOrderResponseDto;
import com.whatap.order.dto.GetOrdersRequestDto;
import com.whatap.order.dto.GetOrdersResponseDto;
import com.whatap.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/app-order/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService service;
  private final ObjectMapper mapper;

  @GetMapping("")
  @ResponseStatus(HttpStatus.OK)
  public ListItemResponseDto<GetOrdersResponseDto> getOrders(@RequestParam Map<String, String> queryParam,
                                                             @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable) {
    GetOrdersRequestDto query = mapper.convertValue(queryParam, GetOrdersRequestDto.class);
    return service.getOrders(query, pageable);
  }

  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public GetOrderResponseDto getOrder(@PathVariable BigInteger id) {
    return service.getOrder(id);
  }
}
