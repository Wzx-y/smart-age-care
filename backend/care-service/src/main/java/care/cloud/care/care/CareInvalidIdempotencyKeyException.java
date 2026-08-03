package care.cloud.care.care;

public class CareInvalidIdempotencyKeyException extends RuntimeException {
    public CareInvalidIdempotencyKeyException() {
        super("护理写操作必须提供长度为 1 至 128 的幂等键");
    }
}
