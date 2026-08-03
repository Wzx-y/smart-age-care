package care.cloud.care.resident;

public class ResidentAttachmentUploadConflictException extends RuntimeException {
    public ResidentAttachmentUploadConflictException() {
        super("附件上传状态或文件校验不符合要求");
    }
}
