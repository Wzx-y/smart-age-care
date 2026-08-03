package care.cloud.care.care;

import jakarta.validation.constraints.NotNull;

public record ChangeCarePlanStatusRequest(@NotNull Long planVersion) {
}
