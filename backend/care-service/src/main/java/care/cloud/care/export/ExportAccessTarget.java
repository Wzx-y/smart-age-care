package care.cloud.care.export;

import java.time.OffsetDateTime;

public record ExportAccessTarget(String accessUrl, OffsetDateTime expiresAt) {
}
