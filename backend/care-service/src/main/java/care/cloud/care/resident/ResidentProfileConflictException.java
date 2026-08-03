package care.cloud.care.resident;

public class ResidentProfileConflictException extends RuntimeException {
    public ResidentProfileConflictException() {
        super("档案子记录已变化，请刷新后重试");
    }
}
