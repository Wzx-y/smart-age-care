package care.cloud.care.care;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarkCareTaskExceptionRequest(@NotNull Long taskVersion, @NotBlank String exceptionReason) {
}
