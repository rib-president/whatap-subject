package com.whatap.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class SuccessResponseDto {
  private Boolean success;
}
