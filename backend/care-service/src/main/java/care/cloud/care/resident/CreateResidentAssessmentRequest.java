package care.cloud.care.resident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateResidentAssessmentRequest(
        @NotBlank(message = "评估类型不能为空") String assessmentType,
        @NotNull(message = "评估日期不能为空") LocalDate assessmentDate,
        Integer score,
        @NotBlank(message = "风险等级不能为空") String riskLevel,
        String note
) {
}
