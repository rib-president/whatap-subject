package com.whatap.product.controller;

import com.whatap.product.dto.GetProductResponseDto;
import com.whatap.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;

@RestController
@RequestMapping("/app-product/products")
@RequiredArgsConstructor
public class ProductController {
  private final ProductService service;

  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public GetProductResponseDto getProduct(@PathVariable BigInteger id) {
    return service.getProduct(id);
  }
}
