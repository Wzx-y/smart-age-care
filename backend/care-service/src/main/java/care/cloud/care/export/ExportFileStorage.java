package care.cloud.care.export;

public interface ExportFileStorage {
    void store(String storageKey, byte[] content);
    ExportAccessTarget signDownload(String storageKey);
}
