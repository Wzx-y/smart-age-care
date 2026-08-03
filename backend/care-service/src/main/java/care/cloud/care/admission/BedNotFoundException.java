package care.cloud.care.admission;

public class BedNotFoundException extends RuntimeException {
    public BedNotFoundException(Long bedId) {
        super("床位不存在：" + bedId);
    }
}
