package io.github.mye19.lock.aspect;

import io.github.mye19.lock.annotation.OptimisticLock;
import io.github.mye19.lock.exception.OptimisticLockRetryExhaustedException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import jakarta.persistence.OptimisticLockException;

/**
 * {@link OptimisticLock} 처리 Aspect.
 *
 * <p>{@link ObjectOptimisticLockingFailureException} 또는 {@link OptimisticLockException}
 * 발생 시 지정된 횟수만큼 재시도합니다. 재시도 간 지연 시간을 두어 충돌을 완화합니다.
 *
 * <p>모든 재시도가 소진되면 {@link OptimisticLockRetryExhaustedException}을 던집니다.
 */
@Slf4j
@Aspect
@Order(3)
public class OptimisticLockAspect {

    @Around("@annotation(optimisticLock)")
    public Object around(ProceedingJoinPoint joinPoint, OptimisticLock optimisticLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();

        int maxRetry = optimisticLock.maxRetry();
        long retryDelayMillis = optimisticLock.retryDelayMillis();

        int attempt = 0;
        Exception lastException = null;

        while (attempt <= maxRetry) {
            try {
                if (attempt > 0) {
                    log.debug("[OptimisticLock] Retry attempt {}/{} on method: {}", attempt, maxRetry, methodName);
                }
                return joinPoint.proceed();

            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                lastException = e;
                attempt++;

                if (attempt > maxRetry) {
                    break;
                }

                log.warn("[OptimisticLock] Version conflict detected on method: {}. Retrying... ({}/{})",
                        methodName, attempt, maxRetry);

                if (retryDelayMillis > 0) {
                    try {
                        Thread.sleep(retryDelayMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
            }
        }

        log.error("[OptimisticLock] All {} retries exhausted on method: {}", maxRetry, methodName);
        throw new OptimisticLockRetryExhaustedException(maxRetry, methodName, lastException);
    }
}
