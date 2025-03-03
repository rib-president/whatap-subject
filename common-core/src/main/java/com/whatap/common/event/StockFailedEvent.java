package com.whatap.common.event;

import com.whatap.common.event.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockFailedEvent extends EventItem implements Event{
  private BigInteger orderId;
  private EventType eventType;

  public StockFailedEvent(BigInteger orderId, EventType eventType, List<Item> items) {
    super(items);
    this.orderId = orderId;
    this.eventType = eventType;
  }

  @Override
  public EventType getEventType() {
    return this.eventType;
  }
}
