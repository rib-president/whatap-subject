package com.whatap.common.event;

import com.whatap.common.event.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent extends EventItem implements Event{
  private BigInteger orderId;

  @Override
  public EventType getEventType() {
    return EventType.ORDER_CANCELLED;
  }
}
