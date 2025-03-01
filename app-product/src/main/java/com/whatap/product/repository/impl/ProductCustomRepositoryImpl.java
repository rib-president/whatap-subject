package com.whatap.product.repository.impl;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.whatap.product.dto.GetProductsByPaginationRequestDto;
import com.whatap.product.entity.Product;
import com.whatap.product.entity.QProduct;
import com.whatap.product.repository.ProductCustomRepository;
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
public class ProductCustomRepositoryImpl implements ProductCustomRepository {
  @PersistenceContext
  private EntityManager em;
  private final JPAQueryFactory factory;

  QProduct product = QProduct.product;

  @Override
  public Page<Product> findAllByCriteria(GetProductsByPaginationRequestDto criteria, Pageable pageable) {
    JPAQuery<Product> query = factory.selectFrom(product);
    JPAQuery<Long> countQuery = factory.select(product.count()).from(product);

    query.where(nameContaining(criteria.getName()),
        priceGte(criteria.getPriceGte()),
        priceLte(criteria.getPriceLte()),
        stockGte(criteria.getStockGte()),
        stockLte(criteria.getStockLte()),
        createdAtGte(criteria.getCreatedAtGte()),
        createdAtLte(criteria.getCreatedAtLte())
        );

    List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
    pageable.getSort().stream()
        .forEach(sort -> {
          Order direction = sort.isAscending() ? Order.ASC : com.querydsl.core.types.Order.DESC;

          switch (sort.getProperty()) {
            case "id": orderSpecifiers.add(new OrderSpecifier<>(direction, product.id)); break;
            case "name": orderSpecifiers.add(new OrderSpecifier<>(direction, product.name)); break;
            case "price": orderSpecifiers.add(new OrderSpecifier<>(direction, product.price)); break;
            case "stock": orderSpecifiers.add(new OrderSpecifier<>(direction, product.stock)); break;
            case "createdAt": orderSpecifiers.add(new OrderSpecifier<>(direction, product.createdAt)); break;
            case "updatedAt": orderSpecifiers.add(new OrderSpecifier<>(direction, product.updatedAt)); break;
          }
        });

    List<Product> products = query.offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(orderSpecifiers.toArray(new OrderSpecifier[]{}))
        .fetch();
    Long total = countQuery.fetchOne();

    return PageableExecutionUtils.getPage(products, pageable, () -> total);
  }

  private BooleanExpression nameContaining(String name) {
    return name != null ?
        product.name.contains(name) : null;
  }

  private BooleanExpression priceGte(BigDecimal price) {
    return price != null ?
        product.price.goe(price) : null;
  }

  private BooleanExpression priceLte(BigDecimal price) {
    return price != null ?
        product.price.loe(price) : null;
  }

  private BooleanExpression stockGte(Integer stock) {
    return stock != null ?
        product.stock.goe(stock) : null;
  }

  private BooleanExpression stockLte(Integer stock) {
    return stock != null ?
        product.stock.loe(stock) : null;
  }

  private BooleanExpression createdAtGte(LocalDate createdAt) {
    return createdAt != null ?
        product.createdAt.goe(createdAt.atStartOfDay()) : null;
  }

  private BooleanExpression createdAtLte(LocalDate createdAt) {
    return createdAt != null ?
        product.createdAt.loe(createdAt.atTime(LocalTime.from(LocalDateTime.MAX))) : null;
  }

}
