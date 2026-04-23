package io.github.mye19.lock;

import io.github.mye19.lock.annotation.DistributedLock;
import io.github.mye19.lock.aspect.DistributedLockAspect;
import io.github.mye19.lock.strategy.SpelKeyParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpelKeyParserTest {

    @Test
    @DisplayName("SpEL 리터럴 + 파라미터 조합 키를 올바르게 파싱한다")
    void parsesSpelExpressionWithParameter() throws NoSuchMethodException {
        // given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{"orderId"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{42L});

        // when
        String key = SpelKeyParser.parse(joinPoint, "'order:' + #orderId");

        // then
        assertThat(key).isEqualTo("order:42");
    }

    @Test
    @DisplayName("객체 필드 접근 SpEL을 파싱한다")
    void parsesSpelWithObjectField() {
        // given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        var user = new UserDto(1001L, "tester");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{"user"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{user});

        // when
        String key = SpelKeyParser.parse(joinPoint, "#user.id");

        // then
        assertThat(key).isEqualTo("1001");
    }

    record UserDto(Long id, String name) {}
}
