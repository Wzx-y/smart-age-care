package care.cloud.care.care;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolveCareTaskFollowUpRequest(@NotNull Long followUpVersion, @NotBlank String resolutionNote) {
}
