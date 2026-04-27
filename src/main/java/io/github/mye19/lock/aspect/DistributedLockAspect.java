package io.github.mye19.lock.aspect;

import io.github.mye19.lock.annotation.DistributedLock;
import io.github.mye19.lock.exception.LockAcquisitionException;
import io.github.mye19.lock.strategy.SpelKeyParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;

/**
 * {@link DistributedLock} 처리 Aspect.
 *
 * <p>Redisson의 공정 락(Fair Lock)을 사용해 분산 환경에서의 동시성을 보장합니다.
 * 락 획득 실패 시 {@link LockAcquisitionException}을 던집니다.
 *
 * <p>트랜잭션 커밋 전에 락이 해제되는 문제를 방지하기 위해
 * {@code @Order(1)}로 트랜잭션 AOP보다 먼저 실행됩니다.
 */
@Slf4j
@Aspect
@Order(1)
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "lock:";

    private final RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String resolvedKey = LOCK_PREFIX + SpelKeyParser.parse(joinPoint, distributedLock.key());

        RLock lock = redissonClient.getFairLock(resolvedKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!acquired) {
                throw new LockAcquisitionException(resolvedKey);
            }

            log.debug("[DistributedLock] Acquired lock: {}", resolvedKey);
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(resolvedKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[DistributedLock] Released lock: {}", resolvedKey);
            }
        }
    }
}
