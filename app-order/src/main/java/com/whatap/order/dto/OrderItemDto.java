package com.whatap.order.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;

@Setter
@Getter
public class OrderItemDto {
  @NotNull
  private BigInteger productId;

  @NotNull
  private Integer quantity;
}
