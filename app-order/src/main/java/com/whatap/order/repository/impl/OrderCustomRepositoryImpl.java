package com.whatap.order.repository.impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.whatap.order.dto.GetOrdersRequestDto;
import com.whatap.order.entity.Order;
import com.whatap.order.entity.QOrder;
import com.whatap.order.entity.QOrderItem;
import com.whatap.order.repository.OrderCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class OrderCustomRepositoryImpl implements OrderCustomRepository {
  @PersistenceContext
  private EntityManager em;
  private final JPAQueryFactory factory;

  QOrder order = QOrder.order;
  QOrderItem orderItem = QOrderItem.orderItem;

  @Override
  public Page<Order> findAllByCriteria(GetOrdersRequestDto criteria, Pageable pageable) {
    JPAQuery<Order> query = factory.selectFrom(order)
        .leftJoin(order.orderItems, orderItem).distinct();
    JPAQuery<Long> countQuery = factory.select(order.count()).from(order)
        .leftJoin(order.orderItems, orderItem).distinct();

    query.where(totalPriceGte(criteria.getTotalPriceGte()),
        totalPriceLte(criteria.getTotalPriceLte()),
        productNameContaining(criteria.getProductName()),
        createdAtGte(criteria.getCreatedAtGte()),
        createdAtLte(criteria.getCreatedAtLte())
    );

    List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
    pageable.getSort().stream()
        .forEach(sort -> {
          com.querydsl.core.types.Order direction = sort.isAscending() ? com.querydsl.core.types.Order.ASC : com.querydsl.core.types.Order.DESC;

          switch (sort.getProperty()) {
            case "id": orderSpecifiers.add(new OrderSpecifier<>(direction, order.id)); break;
            case "totalPrice": orderSpecifiers.add(new OrderSpecifier<>(direction, order.totalPrice)); break;
            case "createdAt": orderSpecifiers.add(new OrderSpecifier<>(direction, order.createdAt)); break;
            case "updatedAt": orderSpecifiers.add(new OrderSpecifier<>(direction, order.updatedAt)); break;
          }
        });

    List<Order> orders = query.offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(orderSpecifiers.toArray(new OrderSpecifier[]{}))
        .fetch();
    Long total = countQuery.fetchOne();

    return PageableExecutionUtils.getPage(orders, pageable, () -> total);
  }

  private BooleanExpression totalPriceGte(BigDecimal totalPrice) {
    return totalPrice != null ?
        order.totalPrice.goe(totalPrice) : null;
  }

  private BooleanExpression totalPriceLte(BigDecimal totalPrice) {
    return totalPrice != null ?
        order.totalPrice.loe(totalPrice) : null;
  }

  private BooleanExpression productNameContaining(String productName) {
    return productName != null ?
        orderItem.productName.contains(productName) : null;
  }

  private BooleanExpression createdAtGte(LocalDate createdAt) {
    return createdAt != null ?
       order.createdAt.goe(createdAt.atStartOfDay()) : null;
  }

  private BooleanExpression createdAtLte(LocalDate createdAt) {
    return createdAt != null ?
       order.createdAt.loe(createdAt.atTime(LocalTime.from(LocalDateTime.MAX))) : null;
  }

}
