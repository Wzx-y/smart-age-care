package care.cloud.care.care;

import java.time.OffsetDateTime;

public record CareIdempotencyRecord(
        Long id,
        Long tenantId,
        Long actorId,
        String operation,
        String idempotencyKey,
        String requestFingerprint,
        CareIdempotencyStatus status,
        CareWriteResult result,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}
