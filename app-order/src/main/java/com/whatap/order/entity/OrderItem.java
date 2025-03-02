package com.whatap.order.entity;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;

@DynamicInsert
@DynamicUpdate
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order__order_item")
@Entity
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
  private BigInteger id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false, columnDefinition = "BIGINT")
  private Order order;

  @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT")
  private BigInteger productId;

  @Column(name = "product_name", nullable = false, length = 32)
  private String productName;

  @Column(name = "product_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal productPrice;

  @Column(name = "quantity")
  @Setter
  private Integer quantity;
}
