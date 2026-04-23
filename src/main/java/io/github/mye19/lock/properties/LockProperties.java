package io.github.mye19.lock.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Lock Starter 설정 프로퍼티.
 *
 * <p>application.yml 예시:
 * <pre>
 * spring:
 *   lock:
 *     distributed:
 *       enabled: true
 *     pessimistic:
 *       enabled: true
 *     optimistic:
 *       enabled: true
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.lock")
public class LockProperties {

    private Distributed distributed = new Distributed();
    private Pessimistic pessimistic = new Pessimistic();
    private Optimistic optimistic = new Optimistic();

    @Getter
    @Setter
    public static class Distributed {
        /** 분산 락 Aspect 활성화 여부. RedissonClient 빈이 없으면 자동으로 비활성화됩니다. */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Pessimistic {
        /** 비관적 락 Aspect 활성화 여부. JPA가 없으면 자동으로 비활성화됩니다. */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Optimistic {
        /** 낙관적 락 재시도 Aspect 활성화 여부. */
        private boolean enabled = true;
    }
}
