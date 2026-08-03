package care.cloud.care.export;

public class ExportStorageUnavailableException extends RuntimeException {
    public ExportStorageUnavailableException() {
        super("Export storage is unavailable");
    }

    public ExportStorageUnavailableException(Throwable cause) {
        super("Export storage is unavailable", cause);
    }
}
