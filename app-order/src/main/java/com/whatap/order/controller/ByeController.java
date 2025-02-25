package com.whatap.order.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/app-order")
@RestController
@Slf4j
public class ByeController {

  @GetMapping("/bye")
  public Mono<String> hello() {
    log.info("bye-request received!");
    return Mono.just("Bye: " + System.currentTimeMillis());
  }
}
