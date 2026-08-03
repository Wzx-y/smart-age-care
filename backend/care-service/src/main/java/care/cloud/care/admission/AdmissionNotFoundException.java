package care.cloud.care.admission;

public class AdmissionNotFoundException extends RuntimeException {
    public AdmissionNotFoundException(Long admissionId) {
        super("入住单不存在：" + admissionId);
    }
}
