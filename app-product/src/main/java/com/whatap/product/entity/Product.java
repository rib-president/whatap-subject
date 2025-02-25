package com.whatap.product.entity;

import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@DynamicInsert
@DynamicUpdate
@Getter
@Table(name = "product")
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

  public void increaseStock(int quantity) {
    this.stock += quantity;
  }

  public Boolean decreaseStock(int quantity) {
    if(this.stock >= quantity) {
      this.stock -= quantity;
      return true;
    }
    return false;
  }
}
