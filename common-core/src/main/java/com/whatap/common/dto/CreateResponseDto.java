package com.whatap.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class CreateResponseDto <T> {
  private T id;
}
