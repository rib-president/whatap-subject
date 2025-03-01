package com.whatap.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatap.common.dto.CreateResponseDto;
import com.whatap.common.dto.ListItemResponseDto;
import com.whatap.common.dto.SuccessResponseDto;
import com.whatap.product.dto.*;
import com.whatap.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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
    GetProductsByPaginationRequestDto query = mapper.convertValue(queryParam, GetProductsByPaginationRequestDto.class);
    return service.getProductsByPagination(query, pageable);
  }

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  public CreateResponseDto<String> addProduct(@Valid @RequestBody AddProductRequestDto body) {
    return service.addProduct(body);
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public SuccessResponseDto updateProduct(@PathVariable BigInteger id, @Valid @RequestBody UpdateProductRequestDto body) {
    return service.updateProduct(id, body);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public SuccessResponseDto deleteProduct(@PathVariable BigInteger id) {
    return service.deleteProduct(id);
  }
}
