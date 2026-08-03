package care.cloud.care.care;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CreateCarePlanTaskTemplateRequest(
        @NotNull Long planId, @NotBlank String taskName, @NotNull LocalTime scheduledTime, @NotNull Long assigneeId
) {
}
