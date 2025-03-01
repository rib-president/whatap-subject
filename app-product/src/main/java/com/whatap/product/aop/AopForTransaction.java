package com.whatap.product.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AOP에서 트랜잭션 분리
 */
@Component
public class AopForTransaction {
  // REQUIRES_NEW: 부모 트랜잭션 유무에 관계 없이 별도 트랜잭션으로 동작
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Object proceed(final ProceedingJoinPoint joinPoint) throws  Throwable {
    return joinPoint.proceed();
  }
}
