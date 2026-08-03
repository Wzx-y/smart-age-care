package care.cloud.care.care;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record UpdateCarePlanTaskTemplateRequest(
        @NotNull Long templateVersion,
        @NotBlank @Size(max = 128) String taskName,
        @NotNull LocalTime scheduledTime,
        @NotNull Long assigneeId
) {
}
