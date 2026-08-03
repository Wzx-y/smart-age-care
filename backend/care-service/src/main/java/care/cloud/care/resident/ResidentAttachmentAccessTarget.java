package care.cloud.care.resident;

import java.time.OffsetDateTime;

public record ResidentAttachmentAccessTarget(String accessUrl, OffsetDateTime expiresAt) {
}
