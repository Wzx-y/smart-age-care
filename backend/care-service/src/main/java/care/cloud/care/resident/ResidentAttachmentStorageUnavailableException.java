package care.cloud.care.resident;

public class ResidentAttachmentStorageUnavailableException extends RuntimeException {
    public ResidentAttachmentStorageUnavailableException() {
        super("附件存储服务暂不可用");
    }

    public ResidentAttachmentStorageUnavailableException(Throwable cause) {
        super("附件存储服务暂不可用", cause);
    }
}
