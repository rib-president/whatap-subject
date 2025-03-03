package com.whatap.order.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Setter
@Getter
public class OrderProductRequestDto {
  @NotBlank
  private String ordererName;
  @NotBlank
  private String ordererPhoneNumber;
  @NotBlank
  private String ordererAddress;

  @NotNull
  @Size(min = 1)
  private List<OrderItemDto> items;
}
