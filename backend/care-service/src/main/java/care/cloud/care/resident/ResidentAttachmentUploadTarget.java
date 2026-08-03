package care.cloud.care.resident;

import java.time.OffsetDateTime;

public record ResidentAttachmentUploadTarget(String uploadUrl, OffsetDateTime expiresAt) {
}
