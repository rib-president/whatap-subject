package com.whatap.order.producer;

import com.whatap.common.event.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderProducer {
  private final KafkaTemplate<String, Event> kafkaTemplate;

  public OrderProducer(KafkaTemplate<String, Event> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void create(Event event) {
    kafkaTemplate.send("order-events", event);
  }
}
