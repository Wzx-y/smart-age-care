package care.cloud.care.care;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateCarePlanRequest(
        @NotNull Long planVersion,
        @NotBlank @Size(max = 128) String planName,
        @NotBlank @Size(max = 128) String frequencyText,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        Long assessmentId
) {
}
