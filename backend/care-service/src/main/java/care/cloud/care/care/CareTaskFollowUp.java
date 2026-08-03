package care.cloud.care.care;

import java.time.OffsetDateTime;

public record CareTaskFollowUp(
        Long id,
        Long tenantId,
        Long taskId,
        Long residentId,
        CareTaskFollowUpStatus status,
        String exceptionReason,
        String resolutionNote,
        Long createdBy,
        OffsetDateTime createdAt,
        Long closedBy,
        OffsetDateTime closedAt,
        long version
) {
}
