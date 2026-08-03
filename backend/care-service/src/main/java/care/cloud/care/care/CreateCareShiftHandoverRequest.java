package care.cloud.care.care;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreateCareShiftHandoverRequest(
        @NotNull LocalDate shiftDate,
        @NotBlank String shiftCode,
        @NotNull Long toUserId,
        @NotBlank String note,
        @NotEmpty List<@NotNull Long> taskIds
) {
}
