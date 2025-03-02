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
        .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND"));

    // 재고가 부족할 경우
    if (product.getStock() < item.getQuantity()) {
      return false;
    }

    // 재고 있음
    product.update(null, null, product.getStock() - item.getQuantity());

    repository.save(product);
    return true;
  }
}
