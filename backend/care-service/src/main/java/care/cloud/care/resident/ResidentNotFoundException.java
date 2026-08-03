package care.cloud.care.resident;

public class ResidentNotFoundException extends RuntimeException {
    public ResidentNotFoundException(Long residentId) {
        super("长者档案不存在：" + residentId);
    }
}
