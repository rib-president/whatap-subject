package com.whatap.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class ListItemResponseDto <T> {
  private long total;
  private long count;
  private long limit;
  private long offset;
  private List<T> items;
}
