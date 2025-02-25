package com.whatap.product.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.GET;
import java.math.BigInteger;

@Setter
@Getter
@Builder
public class GetProductResponseDto {
  private String id;

  private String name;
  private String price;

  private Integer stock;

  private String createdAt;
  private String updatedAt;
}
