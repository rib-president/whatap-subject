package com.whatap.order.repository;

import com.whatap.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;

public interface OrderItemRepository extends JpaRepository<OrderItem, BigInteger> {
}
