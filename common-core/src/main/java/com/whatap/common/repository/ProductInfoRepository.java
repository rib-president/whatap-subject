package com.whatap.common.repository;

import com.whatap.common.entity.ProductInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductInfoRepository extends CrudRepository<ProductInfo, String> {
  Optional<ProductInfo> findById(String id);
}
