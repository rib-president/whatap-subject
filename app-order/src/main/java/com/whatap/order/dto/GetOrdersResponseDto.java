package com.whatap.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class GetOrdersResponseDto {
  private String id;

  private String ordererName;
  private String totalPrice;

  private String item;

  private String createdAt;
  private String updatedAt;
}
