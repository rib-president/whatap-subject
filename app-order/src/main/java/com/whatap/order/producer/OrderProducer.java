package com.whatap.order.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderProducer {
  private final KafkaTemplate<String, String> kafkaTemplate;

  public OrderProducer(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void create(String message) {
    kafkaTemplate.send("order-events", message);
  }
}
