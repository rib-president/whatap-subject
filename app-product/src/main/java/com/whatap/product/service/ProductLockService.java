package com.whatap.product.service;

import com.whatap.common.event.EventItem;
import com.whatap.product.aop.annotation.DistributedLock;
import com.whatap.product.entity.Product;
import com.whatap.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductLockService {
  private final ProductRepository repository;

  @DistributedLock(key = "#item.productId")
  public Boolean updateStockWithLock(EventItem.Item item) {
    Product product = repository.findById(item.getProductId())
        .orElse(null);

    Integer updatedQuantity = item.getQuantity() - item.getLatestQuantity();

    // 삭제된 상품을 업데이트하려하거나 재고가 부족할 경우
    if ((product == null && item.getQuantity() > 0) ||(product != null && product.getStock() < updatedQuantity)) {
      return false;
    }

    // 재고 있음
    return true;
  }
}
