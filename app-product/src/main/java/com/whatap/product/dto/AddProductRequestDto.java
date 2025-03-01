package com.whatap.product.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Setter
@Getter
public class AddProductRequestDto {
  @NotBlank
  private String name;

  @NotNull
  private BigDecimal price;

  @NotNull
  private Integer stock;

}
