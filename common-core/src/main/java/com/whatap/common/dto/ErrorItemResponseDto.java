package com.whatap.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ErrorItemResponseDto {
  private String code;
  private String message;
}
