package care.cloud.care.admission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferBedRequest(
        @NotNull Long targetBedId,
        @NotNull Long admissionVersion,
        @NotNull Long sourceBedVersion,
        @NotNull Long targetBedVersion,
        @NotBlank @Size(max = 500) String reason
) {
}
