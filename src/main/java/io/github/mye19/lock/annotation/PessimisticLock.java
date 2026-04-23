package io.github.mye19.lock.annotation;

import jakarta.persistence.LockModeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JPA 비관적 락 애노테이션.
 *
 * <p>메서드 실행을 트랜잭션으로 감싸고, JPA {@link LockModeType}을 적용합니다.
 * 단일 DB 인스턴스 환경에서 충돌이 잦을 때(재고 차감 등) 사용합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @PessimisticLock
 * public void decreaseStock(Long productId, int quantity) { ... }
 * }</pre>
 *
 * @see io.github.mye19.lock.aspect.PessimisticLockAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PessimisticLock {

    /**
     * JPA 락 모드.
     * 기본값은 {@link LockModeType#PESSIMISTIC_WRITE} (SELECT FOR UPDATE).
     */
    LockModeType lockMode() default LockModeType.PESSIMISTIC_WRITE;

    /**
     * 락 획득 타임아웃(ms). -1이면 DB 기본 설정을 따릅니다.
     */
    long timeoutMillis() default -1L;
}
