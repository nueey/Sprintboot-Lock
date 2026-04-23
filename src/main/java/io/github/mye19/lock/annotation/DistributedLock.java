package io.github.mye19.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 분산 락 애노테이션.
 *
 * <p>메서드 실행 전 Redis에서 락을 획득하고, 실행 완료 후 해제합니다.
 * 분산 환경(다중 인스턴스)에서 동시성을 보장해야 할 때 사용합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @DistributedLock(key = "'order:' + #orderId", waitTime = 3, leaseTime = 5)
 * public void processOrder(Long orderId) { ... }
 * }</pre>
 *
 * @see io.github.mye19.lock.aspect.DistributedLockAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 락 키. SpEL 표현식을 지원합니다.
     * 예: {@code "'stock:' + #productId"}
     */
    String key();

    /**
     * 락 획득 대기 시간. 이 시간 안에 획득하지 못하면 예외를 던집니다.
     */
    long waitTime() default 5L;

    /**
     * 락 보유 최대 시간. 이 시간이 지나면 자동으로 해제됩니다.
     */
    long leaseTime() default 3L;

    /**
     * 시간 단위.
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
