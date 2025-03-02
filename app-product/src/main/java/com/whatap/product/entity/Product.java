package com.whatap.product.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;

@DynamicInsert
@DynamicUpdate
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product__product")
@Entity
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
  private BigInteger id;

  @Column(name = "name", nullable = false, length = 32)
  private String name;

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "stock",  nullable = false)
  private Integer stock;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public void update(String name, BigDecimal price, Integer stock) {
    Optional.ofNullable(name).ifPresent(n -> this.name = n);
    Optional.ofNullable(price).ifPresent(p -> this.price = p);
    Optional.ofNullable(stock).ifPresent(s -> this.stock = s);
  }
}
