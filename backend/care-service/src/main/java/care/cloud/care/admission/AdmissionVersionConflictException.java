package care.cloud.care.admission;

public class AdmissionVersionConflictException extends RuntimeException {
    public AdmissionVersionConflictException() {
        super("入住或床位信息已发生变化，请刷新后重试");
    }
}
