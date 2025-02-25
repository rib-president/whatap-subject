package com.whatap.product.repository;

import com.whatap.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;

public interface ProductRepository extends JpaRepository<Product, BigInteger>, ProductCustomRepository {
}
