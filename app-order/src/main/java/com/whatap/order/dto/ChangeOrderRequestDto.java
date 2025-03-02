package com.whatap.order.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigInteger;
import java.util.List;

@Setter
@Getter
public class ChangeOrderRequestDto {
  @NotNull
  @Size(min = 1)
  private List<Item> items;

  @Setter
  @Getter
  public static class Item {
    @NotNull
    private BigInteger id;

    @NotNull
    private Integer quantity;
  }
}
