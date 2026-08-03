package care.cloud.care.resident;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateResidentAttachmentRequest(
        @NotBlank(message = "文件名不能为空") String fileName,
        @NotBlank(message = "文件类型不能为空") String contentType,
        @Positive(message = "文件大小必须大于 0") @Max(value = 52_428_800L, message = "单个附件不能超过 50 MB") long byteSize
) {
}
