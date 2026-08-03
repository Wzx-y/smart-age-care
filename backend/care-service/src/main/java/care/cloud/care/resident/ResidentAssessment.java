package care.cloud.care.resident;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ResidentAssessment(
        Long id,
        Long residentId,
        String assessmentType,
        LocalDate assessmentDate,
        Integer score,
        String riskLevel,
        String note,
        Long assessorId,
        long version,
        OffsetDateTime createdAt
) {
}
