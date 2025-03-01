package com.whatap.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.whatap.common", "com.whatap.product"})
@EnableJpaRepositories(basePackages = {"com.whatap.product.repository", "com.whatap.common.repository"})
@EntityScan(basePackages = {"com.whatap.**.entity"})
@EnableRedisRepositories(basePackages = "com.whatap.common.repository")
@EnableEurekaClient
public class ProductApplication {
  public static void main(String[] args) {
    SpringApplication.run(ProductApplication.class, args);
  }
}