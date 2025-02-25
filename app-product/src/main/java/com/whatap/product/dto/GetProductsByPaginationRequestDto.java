package com.whatap.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
public class GetProductsByPaginationRequestDto {
  private String name;

  @JsonProperty("price:gte")
  private BigDecimal priceGte;
  @JsonProperty("price:lte")
  private BigDecimal priceLte;

  @JsonProperty("stock:gte")
  private Integer stockGte;
  @JsonProperty("stock:lte")
  private Integer stockLte;

  @JsonProperty("createdAt:gte")
  private LocalDate createdAtGte;
  @JsonProperty("createdAt:lte")
  private LocalDate createdAtLte;
}


