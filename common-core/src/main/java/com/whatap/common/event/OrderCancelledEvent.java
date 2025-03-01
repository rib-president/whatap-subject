package com.whatap.common.event;

import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class OrderCancelledEvent extends EventItem {

  OrderCancelledEvent(List<Item> items) {
    super(items);
  }
}
