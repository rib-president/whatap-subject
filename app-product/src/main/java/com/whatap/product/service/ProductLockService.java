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

  @DistributedLock(key = "#item.productId") // redisson을 사용한 분산락 적용 어노테이션(상품의 id가 고유키로 사용됨)
  public Boolean updateStockWithLock(EventItem.Item item) {
    // id를 가진 product entity 가져오기
    Product product = repository.findById(item.getProductId())
        .orElse(null);

    // 차감하려는 수량
    Integer updatedQuantity = item.getQuantity() - item.getLatestQuantity();

    // 삭제된 상품을 업데이트하려하거나 재고가 부족할 경우 - 변경불가
    if ((product == null && item.getQuantity() > 0) ||(product != null && product.getStock() < updatedQuantity)) {
      return false;
    }

    // 변경가능
    return true;
  }
}
