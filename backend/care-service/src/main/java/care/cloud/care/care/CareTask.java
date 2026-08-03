package care.cloud.care.care;

import java.time.OffsetDateTime;

public record CareTask(Long id, Long tenantId, Long planId, Long residentId, String taskName, OffsetDateTime scheduledAt,
                       Long assigneeId, CareTaskStatus status, String exceptionReason, long version) {
}
