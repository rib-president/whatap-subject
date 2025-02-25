package com.whatap.product.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/app-product")
@RestController
@Slf4j
public class HelloController {

  @GetMapping("/hello")
  public Mono<String> hello() {
    log.info("hello-request received!");
    return Mono.just("Hello: " + System.currentTimeMillis());
  }
}
