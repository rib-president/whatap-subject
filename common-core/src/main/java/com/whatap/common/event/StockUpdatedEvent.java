package com.whatap.common.event;

import com.whatap.common.event.enums.EventType;
import lombok.*;

import java.math.BigInteger;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdatedEvent implements Event{
  private BigInteger orderId;
  private Boolean isSuccess;

  @Override
  public EventType getEventType() {
    return EventType.STOCK_UPDATED;
  }
}
