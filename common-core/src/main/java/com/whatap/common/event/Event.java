package com.whatap.common.event;

import com.whatap.common.event.enums.EventType;

import java.math.BigInteger;

public interface Event {
  EventType getEventType();
  BigInteger getOrderId();
}
