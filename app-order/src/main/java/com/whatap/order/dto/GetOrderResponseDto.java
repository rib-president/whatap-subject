package com.whatap.order.dto;

import com.whatap.order.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class GetOrderResponseDto {
  private String id;

  private OrderStatus status;

  private String ordererName;
  private String ordererPhoneNumber;
  private String ordererAddress;

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
