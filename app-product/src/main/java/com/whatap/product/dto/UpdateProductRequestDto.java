package com.whatap.product.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class UpdateProductRequestDto {
  private String name;

  private Integer stock;

  private BigDecimal price;
}
