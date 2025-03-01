package com.whatap.common.event;

import lombok.*;

import java.math.BigInteger;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdatedEvent {
  private BigInteger orderId;
  private Boolean isSuccess;
}
