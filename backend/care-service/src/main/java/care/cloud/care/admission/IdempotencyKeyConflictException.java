package care.cloud.care.admission;

public class IdempotencyKeyConflictException extends RuntimeException {
    public IdempotencyKeyConflictException() {
        super("幂等键已用于不同的入住请求，请生成新的请求键");
    }
}
