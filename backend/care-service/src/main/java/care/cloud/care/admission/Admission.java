package care.cloud.care.admission;

import java.time.OffsetDateTime;

public record Admission(
        Long id,
        Long tenantId,
        Long residentId,
        Long bedId,
        OffsetDateTime reservedUntil,
        AdmissionStatus status,
        long version,
        OffsetDateTime appliedAt
) {
}
