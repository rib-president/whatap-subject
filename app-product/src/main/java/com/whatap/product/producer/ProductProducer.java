package com.whatap.product.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductProducer {
  private final KafkaTemplate<String, String> kafkaTemplate;

  public ProductProducer(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void create(String message) {
    kafkaTemplate.send("product-events", message);
  }
}
