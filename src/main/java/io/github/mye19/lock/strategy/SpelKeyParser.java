package io.github.mye19.lock.strategy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL(Spring Expression Language) 기반 락 키 파서.
 *
 * <p>애노테이션의 key 속성에서 SpEL 표현식을 평가해 실제 락 키 문자열을 반환합니다.
 *
 * <p>예시:
 * <pre>
 *   key = "'order:' + #orderId"  →  "order:42"
 *   key = "#user.id"             →  "1001"
 * </pre>
 */
public class SpelKeyParser {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private SpelKeyParser() {}

    /**
     * JoinPoint의 메서드 파라미터를 컨텍스트에 바인딩하여 SpEL 표현식을 평가합니다.
     *
     * @param joinPoint AOP JoinPoint
     * @param keyExpression SpEL 표현식 문자열
     * @return 평가된 락 키
     */
    public static String parse(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        Object value = PARSER.parseExpression(keyExpression).getValue(context);
        return value != null ? value.toString() : keyExpression;
    }
}
