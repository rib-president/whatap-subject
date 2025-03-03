package com.whatap.common.event;

import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventItem {
  private List<Item> items;

  @Setter
  @Getter
  @NoArgsConstructor
  public static class Item {
    private BigInteger productId;

    private String productName;
    private BigDecimal productPrice;

    private Integer quantity;

    private Integer latestQuantity;

    public Item(BigInteger productId, Integer quantity, Integer latestQuantity) {
      this.productId = productId;
      this.quantity = quantity;
      this.latestQuantity = latestQuantity;
    }
  }
}
