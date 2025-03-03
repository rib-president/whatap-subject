package com.whatap.product.aop;

import com.whatap.product.aop.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {
  private static final String REDISSON_LOCK_PREFIX = "LOCK:";

  private final RedissonClient redissonClient;
  private final AopForTransaction aopForTransaction;

  @Around("@annotation(com.whatap.product.aop.annotation.DistributedLock)")
  public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

    // 어노테이션의 key의 값을 가져와 고유키 생성(ex. LOCK:1)
    String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());

    RLock rLock = redissonClient.getLock(key);

    try {
      // 락 획득 시도
      boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
      if (!available) {
        // 획득 실패
        return false;
      }

      // 별도 트랜잭션에서 동작(비즈니스 로직 트랜잭션이 롤백되어도 락이 풀리는 것 방지)
      return aopForTransaction.proceed(joinPoint);
    } catch (InterruptedException e) {
      throw new InterruptedException();
    } finally {
      try {
        // 트랜잭션에서 커밋 후 락 해제
        rLock.unlock();
      } catch (IllegalArgumentException e) {
        log.info("Redisson Lock Already UnLock {} {}", method.getName(), key);
      }
    }
  }
}
