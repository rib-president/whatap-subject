package com.whatap.order.repository;

import com.whatap.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;

public interface OrderRepository extends JpaRepository<Order, BigInteger>, OrderCustomRepository {
}
