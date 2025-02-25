package com.whatap.product.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatap.common.dto.ListItemResponseDto;
import com.whatap.product.dto.GetProductResponseDto;
import com.whatap.product.dto.GetProductsByPaginationRequestDto;
import com.whatap.product.dto.GetProductsByPaginationResponseDto;
import com.whatap.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/app-product/products")
@RequiredArgsConstructor
public class ProductController {
  private final ProductService service;
  private final ObjectMapper mapper;

  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public GetProductResponseDto getProduct(@PathVariable BigInteger id) {
    return service.getProduct(id);
  }

  @GetMapping("")
  @ResponseStatus(HttpStatus.OK)
  public ListItemResponseDto<GetProductsByPaginationResponseDto> getProductsByPagination(@RequestParam Map<String, String> queryParam,
                                                                                         @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable) {
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    GetProductsByPaginationRequestDto query = mapper.convertValue(queryParam, GetProductsByPaginationRequestDto.class);
    return service.getProductsByPagination(query, pageable);
  }
}
