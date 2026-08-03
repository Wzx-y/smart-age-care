package care.cloud.care.resident;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Resident(
        Long id,
        Long tenantId,
        String name,
        String gender,
        LocalDate birthDate,
        ResidentStatus status,
        Long currentBedId,
        String emergencyContactName,
        String emergencyContactPhone,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
