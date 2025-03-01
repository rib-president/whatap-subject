package com.whatap.common.event;

import lombok.*;

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
  @AllArgsConstructor
  public static class Item {
    private BigInteger productId;

    private Integer quantity;
  }
}
