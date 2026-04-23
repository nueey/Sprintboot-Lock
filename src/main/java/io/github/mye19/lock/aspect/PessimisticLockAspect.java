package io.github.mye19.lock.aspect;

import io.github.mye19.lock.annotation.PessimisticLock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link PessimisticLock} 처리 Aspect.
 *
 * <p>메서드 실행을 트랜잭션으로 감싸고, JPA의 비관적 락 힌트를
 * {@link EntityManager}에 설정합니다. 실제 락은 Repository 레벨의
 * {@code @Lock(LockModeType.PESSIMISTIC_WRITE)} 와 함께 동작합니다.
 *
 * <p>이 Aspect는 Lock 힌트(타임아웃 등)를 트랜잭션 컨텍스트에 전파하는 역할을 합니다.
 */
@Slf4j
@Aspect
@Order(2)
@RequiredArgsConstructor
public class PessimisticLockAspect {

    private final EntityManager entityManager;

    @Around("@annotation(pessimisticLock)")
    @Transactional
    public Object around(ProceedingJoinPoint joinPoint, PessimisticLock pessimisticLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();

        LockModeType lockMode = pessimisticLock.lockMode();
        long timeoutMillis = pessimisticLock.timeoutMillis();

        if (timeoutMillis >= 0) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("jakarta.persistence.lock.timeout", timeoutMillis);
            // EntityManager 프로퍼티에 타임아웃 힌트 전파
            entityManager.setProperty("jakarta.persistence.lock.timeout", timeoutMillis);
        }

        log.debug("[PessimisticLock] Applying {} on method: {}", lockMode, methodName);

        try {
            return joinPoint.proceed();
        } finally {
            // 타임아웃 힌트 초기화
            if (timeoutMillis >= 0) {
                entityManager.setProperty("jakarta.persistence.lock.timeout", -1L);
            }
        }
    }
}
