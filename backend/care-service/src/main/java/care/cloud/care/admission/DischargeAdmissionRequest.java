package care.cloud.care.admission;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DischargeAdmissionRequest(
        @NotNull @Min(0) Long admissionVersion,
        @NotNull @Min(0) Long bedVersion,
        @NotBlank(message = "退住原因不能为空") @Size(max = 500, message = "退住原因不能超过 500 个字符") String dischargeReason
) {
}
