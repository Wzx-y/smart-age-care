package care.cloud.care.care;

import jakarta.validation.constraints.NotNull;

public record ChangeCarePlanTaskTemplateStatusRequest(@NotNull Long templateVersion) {
}
