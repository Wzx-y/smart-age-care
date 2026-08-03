package care.cloud.care.resident;

public interface ResidentAttachmentStorage {
    ResidentAttachmentUploadTarget signUpload(String storageKey, String contentType);

    ResidentAttachmentAccessTarget signDownload(String storageKey, boolean inline);

    StoredObject inspect(String storageKey);

    record StoredObject(String contentType, long byteSize) {
    }
}
