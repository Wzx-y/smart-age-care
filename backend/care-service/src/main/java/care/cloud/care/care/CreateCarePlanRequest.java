package care.cloud.care.care;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateCarePlanRequest(@NotNull Long residentId, @NotBlank String planName, @NotBlank String frequencyText,
                                    @NotNull LocalDate startDate, LocalDate endDate, Long assessmentId) {
}
