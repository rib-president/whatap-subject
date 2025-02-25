package com.whatap.product.service;

import com.whatap.common.dto.ListItemResponseDto;
import com.whatap.product.dto.GetProductResponseDto;
import com.whatap.product.dto.GetProductsByPaginationRequestDto;
import com.whatap.product.dto.GetProductsByPaginationResponseDto;
import com.whatap.product.entity.Product;
import com.whatap.product.repository.ProductRepository;
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
public class ProductService {
  private final ProductRepository repository;

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
}
