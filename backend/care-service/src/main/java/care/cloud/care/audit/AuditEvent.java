package care.cloud.care.audit;

import java.time.OffsetDateTime;

public record AuditEvent(String source, Long eventId, String action, String resourceType, Long resourceId,
                         Long relatedResidentId, Long actorId, OffsetDateTime createdAt) {
}
