package care.cloud.care.care;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateCareTaskRequest(@NotNull Long planId, @NotBlank String taskName, @NotNull OffsetDateTime scheduledAt,
                                    @NotNull Long assigneeId) {
}
