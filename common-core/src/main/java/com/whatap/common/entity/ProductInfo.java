package com.whatap.common.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash(value = "product_info", timeToLive = -1)
public class ProductInfo {
  @Id
  private String id;

  private String name;
  private BigDecimal price;

}
