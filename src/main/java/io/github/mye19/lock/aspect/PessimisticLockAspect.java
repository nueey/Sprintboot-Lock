package io.github.mye19.lock.aspect;

import io.github.mye19.lock.annotation.PessimisticLock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link PessimisticLock} 처리 Aspect.
 *
 * <p>메서드 실행을 트랜잭션으로 감싸고, JPA의 비관적 락 힌트를
 * {@link EntityManager}에 설정합니다. 실제 락은 Repository 레벨의
 * {@code @Lock(LockModeType.PESSIMISTIC_WRITE)} 와 함께 동작합니다.
 *
 * <p>{@code @Aspect} 빈은 Spring AOP 프록시 대상에서 제외되므로
 * {@code @Transactional} 대신 {@link TransactionTemplate}으로 트랜잭션을 직접 제어합니다.
 */
@Slf4j
@Aspect
@Order(2)
public class PessimisticLockAspect {

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public PessimisticLockAspect(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Around("@annotation(pessimisticLock)")
    public Object around(ProceedingJoinPoint joinPoint, PessimisticLock pessimisticLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();

        LockModeType lockMode = pessimisticLock.lockMode();
        long timeoutMillis = pessimisticLock.timeoutMillis();

        log.debug("[PessimisticLock] Applying {} on method: {}", lockMode, methodName);

        Throwable[] caughtThrowable = {null};

        Object result = transactionTemplate.execute(status -> {
            if (timeoutMillis >= 0) {
                entityManager.setProperty("jakarta.persistence.lock.timeout", timeoutMillis);
            }
            try {
                return joinPoint.proceed();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                caughtThrowable[0] = e;
                status.setRollbackOnly();
                return null;
            } finally {
                if (timeoutMillis >= 0) {
                    entityManager.setProperty("jakarta.persistence.lock.timeout", -1L);
                }
            }
        });

        if (caughtThrowable[0] != null) {
            throw caughtThrowable[0];
        }

        return result;
    }
}
