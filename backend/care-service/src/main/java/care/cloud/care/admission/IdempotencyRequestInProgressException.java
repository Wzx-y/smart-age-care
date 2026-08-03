package care.cloud.care.admission;

public class IdempotencyRequestInProgressException extends RuntimeException {
    public IdempotencyRequestInProgressException() {
        super("相同请求正在处理中，请稍后重试");
    }
}
