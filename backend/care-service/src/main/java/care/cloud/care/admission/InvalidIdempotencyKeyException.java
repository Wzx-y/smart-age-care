package care.cloud.care.admission;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super("幂等键不能为空且长度不能超过 128 位");
    }
}
