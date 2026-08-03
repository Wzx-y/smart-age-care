package care.cloud.care.admission;

import java.time.OffsetDateTime;

public record AdmissionIdempotencyRecord(
        Long id,
        Long tenantId,
        Long actorId,
        String operation,
        String idempotencyKey,
        String requestFingerprint,
        AdmissionIdempotencyStatus status,
        Admission result,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}
