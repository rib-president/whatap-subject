package com.whatap.product.repository;

import com.whatap.product.dto.GetProductsByPaginationRequestDto;
import com.whatap.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductCustomRepository {
  Page<Product> findAllByCriteria(GetProductsByPaginationRequestDto criteria, Pageable pageable);
}
