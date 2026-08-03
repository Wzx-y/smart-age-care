package care.cloud.care.care;

public class CareIdempotencyRequestInProgressException extends RuntimeException {
    public CareIdempotencyRequestInProgressException() {
        super("相同护理请求正在处理中，请稍后重试");
    }
}
