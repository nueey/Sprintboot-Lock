package io.github.mye19.lock.config;

import io.github.mye19.lock.aspect.DistributedLockAspect;
import io.github.mye19.lock.aspect.OptimisticLockAspect;
import io.github.mye19.lock.aspect.PessimisticLockAspect;
import io.github.mye19.lock.properties.LockProperties;
import jakarta.persistence.EntityManager;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Lock Starter 자동 구성 클래스.
 *
 * <p>클래스패스와 프로퍼티 조건에 따라 각 Aspect를 선택적으로 등록합니다.
 * <ul>
 *   <li>{@link DistributedLockAspect}: RedissonClient 빈이 존재할 때만 등록</li>
 *   <li>{@link PessimisticLockAspect}: JPA(EntityManager)가 클래스패스에 있을 때만 등록</li>
 *   <li>{@link OptimisticLockAspect}: 항상 등록 (별도 의존성 불필요)</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(LockProperties.class)
public class LockAutoConfiguration {

    // ── Distributed Lock ──────────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "spring.lock.distributed", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DistributedLockAspect distributedLockAspect(RedissonClient redissonClient) {
        return new DistributedLockAspect(redissonClient);
    }

    // ── Pessimistic Lock ──────────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(EntityManager.class)
    @ConditionalOnBean({EntityManager.class, PlatformTransactionManager.class})
    @ConditionalOnProperty(prefix = "spring.lock.pessimistic", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PessimisticLockAspect pessimisticLockAspect(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        return new PessimisticLockAspect(entityManager, transactionManager);
    }

    // ── Optimistic Lock ───────────────────────────────────────────────────────

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.lock.optimistic", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OptimisticLockAspect optimisticLockAspect() {
        return new OptimisticLockAspect();
    }
}
