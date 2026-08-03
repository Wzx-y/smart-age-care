package care.cloud.care.care;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StartCareTaskRequest(@NotNull @Min(0) Long taskVersion) {
}
