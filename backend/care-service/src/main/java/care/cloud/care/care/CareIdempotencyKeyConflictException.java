package care.cloud.care.care;

public class CareIdempotencyKeyConflictException extends RuntimeException {
    public CareIdempotencyKeyConflictException() {
        super("幂等键已用于不同的护理请求，请生成新的请求键");
    }
}
