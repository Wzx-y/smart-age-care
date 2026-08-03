package care.cloud.care.care;

public class CareStateConflictException extends RuntimeException {
    public CareStateConflictException() { super("护理计划或任务状态已变化，请刷新后重试"); }
}
