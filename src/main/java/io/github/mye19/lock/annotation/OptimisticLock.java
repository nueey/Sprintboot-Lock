package io.github.mye19.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JPA 낙관적 락 재시도 애노테이션.
 *
 * <p>{@link jakarta.persistence.OptimisticLockException} 또는
 * {@link org.springframework.orm.ObjectOptimisticLockingFailureException} 발생 시
 * 지정 횟수만큼 자동 재시도합니다. 충돌이 드문 시나리오(좋아요, 조회수 등)에 적합합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @OptimisticLock(maxRetry = 3, retryDelayMillis = 100)
 * public void increaseLikeCount(Long postId) { ... }
 * }</pre>
 *
 * @see io.github.mye19.lock.aspect.OptimisticLockAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptimisticLock {

    /**
     * 최대 재시도 횟수. 초과 시 {@link io.github.mye19.lock.exception.OptimisticLockRetryExhaustedException}을 던집니다.
     */
    int maxRetry() default 3;

    /**
     * 재시도 간격(ms).
     */
    long retryDelayMillis() default 50L;
}
