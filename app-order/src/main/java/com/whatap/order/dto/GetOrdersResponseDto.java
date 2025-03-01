package com.whatap.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class GetOrdersResponseDto {
  private String id;

  private String totalPrice;

  private List<Item> items;

  private String createdAt;
  private String updatedAt;

  @Setter
  @Getter
  @Builder
  public static class Item {
    private String id;
    private String productName;
    private String productPrice;

    private Integer quantity;
  }
}
