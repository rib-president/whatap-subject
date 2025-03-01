package com.whatap.order.repository;

import com.whatap.order.dto.GetOrdersRequestDto;
import com.whatap.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderCustomRepository {
  Page<Order> findAllByCriteria(GetOrdersRequestDto criteria, Pageable pageable);
}
