package com.whatap.common.event.enums;

import lombok.Getter;

@Getter
public enum EventType {
  ORDER_CREATED, ORDER_UPDATED, ORDER_CANCELLED, STOCK_UPDATED, STOCK_FAILED, STOCK_ROLLBACK
}
