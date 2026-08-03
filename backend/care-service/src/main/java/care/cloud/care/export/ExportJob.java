package care.cloud.care.export;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ExportJob(Long id, Long tenantId, ExportType exportType, LocalDate periodStart, LocalDate periodEnd,
                        ExportStatus status, String fileName, String storageKey, Long createdBy, OffsetDateTime createdAt,
                        OffsetDateTime completedAt, OffsetDateTime expiresAt, long version) {
}
