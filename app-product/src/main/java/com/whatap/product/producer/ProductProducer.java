package com.whatap.product.producer;

import com.whatap.common.event.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductProducer {
  private final KafkaTemplate<String, Event> kafkaTemplate;

  public ProductProducer(KafkaTemplate<String, Event> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void create(Event event) {
    kafkaTemplate.send("product-events", event);
  }
}
