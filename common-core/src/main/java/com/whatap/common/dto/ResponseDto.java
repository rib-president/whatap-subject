package com.whatap.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Builder
@Getter
@Setter
@ToString
public class ResponseDto {
  private Object id;
  private String uri;
  private Object data;
  private List<ErrorItemResponseDto> errors;
}
