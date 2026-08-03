package care.cloud.care.care;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateCareTasksRequest(@NotNull LocalDate serviceDate) {
}
