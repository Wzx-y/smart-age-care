package care.cloud.care.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBedRequest(
        @NotNull Long roomId,
        @NotBlank @Size(max = 64) String bedNo,
        @Size(max = 500) String equipmentSummary
) {
}
