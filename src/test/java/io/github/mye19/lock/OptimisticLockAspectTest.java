package io.github.mye19.lock;

import io.github.mye19.lock.annotation.OptimisticLock;
import io.github.mye19.lock.aspect.OptimisticLockAspect;
import io.github.mye19.lock.exception.OptimisticLockRetryExhaustedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = OptimisticLockAspectTest.TestConfig.class)
class OptimisticLockAspectTest {

    @Autowired
    private SampleService sampleService;

    @Test
    @DisplayName("버전 충돌이 maxRetry 이내에 해소되면 성공한다")
    void retrySucceeds_whenConflictResolvesWithinMaxRetry() {
        // given: 처음 2번 충돌, 3번째 성공
        sampleService.resetCallCount();

        // when
        sampleService.succeedOnThirdAttempt();

        // then
        assertThat(sampleService.getCallCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("maxRetry 초과 시 OptimisticLockRetryExhaustedException이 발생한다")
    void throwsException_whenRetriesExhausted() {
        assertThatThrownBy(() -> sampleService.alwaysFail())
                .isInstanceOf(OptimisticLockRetryExhaustedException.class)
                .hasMessageContaining("retry exhausted");
    }

    @Test
    @DisplayName("낙관적 락 예외가 아닌 경우 재시도 없이 즉시 전파된다")
    void doesNotRetry_onNonOptimisticLockException() {
        sampleService.resetCallCount();
        assertThatThrownBy(() -> sampleService.throwIllegalState())
                .isInstanceOf(IllegalStateException.class);
        assertThat(sampleService.getCallCount()).isEqualTo(1);
    }

    // ── Test Fixtures ─────────────────────────────────────────────────────────

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        OptimisticLockAspect optimisticLockAspect() {
            return new OptimisticLockAspect();
        }

        @Bean
        SampleService sampleService() {
            return new SampleService();
        }
    }

    static class SampleService {

        private final AtomicInteger callCount = new AtomicInteger(0);

        void resetCallCount() {
            callCount.set(0);
        }

        int getCallCount() {
            return callCount.get();
        }

        @OptimisticLock(maxRetry = 3, retryDelayMillis = 0)
        public void succeedOnThirdAttempt() {
            int count = callCount.incrementAndGet();
            if (count < 3) {
                throw new ObjectOptimisticLockingFailureException(Object.class, null);
            }
        }

        @OptimisticLock(maxRetry = 2, retryDelayMillis = 0)
        public void alwaysFail() {
            callCount.incrementAndGet();
            throw new ObjectOptimisticLockingFailureException(Object.class, null);
        }

        @OptimisticLock(maxRetry = 3, retryDelayMillis = 0)
        public void throwIllegalState() {
            callCount.incrementAndGet();
            throw new IllegalStateException("not a lock exception");
        }
    }
}
