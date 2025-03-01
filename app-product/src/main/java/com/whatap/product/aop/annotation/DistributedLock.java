package com.whatap.product.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
  String key(); // lock key

  TimeUnit timeUnit() default TimeUnit.SECONDS; // lock 시간 단위

  long waitTime() default  5L;  // lock 획득 대기 시간(5s)

  long leaseTime() default 3L;  // lock 임대 시간(3s) -> 이후 락 해제
}
