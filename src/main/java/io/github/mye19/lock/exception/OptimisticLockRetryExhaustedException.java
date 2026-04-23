package io.github.mye19.lock.exception;

/**
 * 낙관적 락 재시도 횟수를 초과했을 때 발생하는 예외.
 *
 * <p>{@code @OptimisticLock(maxRetry = N)}에서 N번 모두 실패하면 던져집니다.
 */
public class OptimisticLockRetryExhaustedException extends RuntimeException {

    public OptimisticLockRetryExhaustedException(int maxRetry, String methodName) {
        super(String.format(
                "Optimistic lock retry exhausted after %d attempts on method: %s",
                maxRetry, methodName
        ));
    }

    public OptimisticLockRetryExhaustedException(int maxRetry, String methodName, Throwable cause) {
        super(String.format(
                "Optimistic lock retry exhausted after %d attempts on method: %s",
                maxRetry, methodName
        ), cause);
    }
}
