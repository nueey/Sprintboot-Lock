package io.github.mye19.lock.exception;

/**
 * 락 획득에 실패했을 때 발생하는 예외.
 *
 * <p>{@code @DistributedLock}의 waitTime 안에 Redis 락을 확보하지 못한 경우 던져집니다.
 */
public class LockAcquisitionException extends RuntimeException {

    public LockAcquisitionException(String lockKey) {
        super("Failed to acquire lock for key: " + lockKey);
    }

    public LockAcquisitionException(String lockKey, Throwable cause) {
        super("Failed to acquire lock for key: " + lockKey, cause);
    }
}
