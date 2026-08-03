package care.cloud.care.admission;

import jakarta.validation.constraints.NotNull;

public record ConfirmAdmissionRequest(
        @NotNull(message = "入住单版本不能为空") Long admissionVersion,
        @NotNull(message = "床位版本不能为空") Long bedVersion
) {
}
