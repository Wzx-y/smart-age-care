package care.cloud.care.admission;

import java.time.OffsetDateTime;

public record AdmissionAuditEvent(String action, Long actorId, Long relatedBedId, OffsetDateTime occurredAt) {
}
