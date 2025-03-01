package com.whatap.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
public class GetOrdersRequestDto {
  @JsonProperty("totalPrice:gte")
  private BigDecimal totalPriceGte;
  @JsonProperty("totalPrice:lte")
  private BigDecimal totalPriceLte;

  @JsonProperty("productName:like")
  private String productName;

  @JsonProperty("createdAt:gte")
  private LocalDate createdAtGte;
  @JsonProperty("createdAt:lte")
  private LocalDate createdAtLte;
}
