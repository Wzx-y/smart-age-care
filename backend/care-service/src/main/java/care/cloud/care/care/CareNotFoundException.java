package care.cloud.care.care;

public class CareNotFoundException extends RuntimeException {
    public CareNotFoundException(String type, Long id) { super(type + "不存在或无权访问"); }
}
