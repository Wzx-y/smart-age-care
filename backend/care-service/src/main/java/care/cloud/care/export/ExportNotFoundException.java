package care.cloud.care.export;

public class ExportNotFoundException extends RuntimeException {
    public ExportNotFoundException(Long id) {
        super("Export not found: " + id);
    }
}
