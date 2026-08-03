package care.cloud.care.care;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CareTaskGenerationRun(Long id, Long tenantId, LocalDate serviceDate, int generatedTaskCount, Long createdBy, OffsetDateTime createdAt) {
}
