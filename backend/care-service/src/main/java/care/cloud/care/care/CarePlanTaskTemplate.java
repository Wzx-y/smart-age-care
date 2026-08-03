package care.cloud.care.care;

import java.time.LocalTime;

public record CarePlanTaskTemplate(
        Long id, Long tenantId, Long planId, Long residentId, String taskName, LocalTime scheduledTime,
        Long assigneeId, boolean active, long version
) {
}
