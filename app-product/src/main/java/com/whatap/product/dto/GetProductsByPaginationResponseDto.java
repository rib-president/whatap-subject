package com.whatap.product.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class GetProductsByPaginationResponseDto {
  private String id;

  private String name;
  private String price;

  private Integer stock;

  private String createdAt;
  private String updatedAt;
}
