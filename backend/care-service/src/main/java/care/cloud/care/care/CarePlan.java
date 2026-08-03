package care.cloud.care.care;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CarePlan(Long id, Long tenantId, Long residentId, Long assessmentId, String planName, String frequencyText, Long ownerId,
                       CarePlanStatus status, long version, LocalDate startDate, LocalDate endDate,
                       OffsetDateTime publishedAt, OffsetDateTime createdAt) {
}
