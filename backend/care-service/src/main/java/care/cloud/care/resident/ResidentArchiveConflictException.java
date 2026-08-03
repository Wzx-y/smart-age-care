package care.cloud.care.resident;

public class ResidentArchiveConflictException extends RuntimeException {
    public ResidentArchiveConflictException() {
        super("在院长者必须先完成退住或死亡结案，不能直接归档");
    }
}
