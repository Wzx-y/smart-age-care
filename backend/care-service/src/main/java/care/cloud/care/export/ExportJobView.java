package care.cloud.care.export;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ExportJobView(Long id, ExportType exportType, LocalDate periodStart, LocalDate periodEnd, ExportStatus status,
                            String fileName, OffsetDateTime createdAt, OffsetDateTime completedAt, OffsetDateTime expiresAt, long version) {
    public static ExportJobView from(ExportJob job) {
        return new ExportJobView(job.id(), job.exportType(), job.periodStart(), job.periodEnd(), job.status(), job.fileName(),
                job.createdAt(), job.completedAt(), job.expiresAt(), job.version());
    }
}
