package care.cloud.care.care;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompleteCareTaskRequest(@NotNull Long taskVersion, @NotBlank String resultNote) {
}
