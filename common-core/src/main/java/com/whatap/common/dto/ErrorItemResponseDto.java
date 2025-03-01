package com.whatap.common.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorItemResponseDto {
  private String code;
  private String message;
}
