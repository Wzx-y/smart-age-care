package care.cloud.care.admission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecideAdmissionAssessmentRequest(
        @NotNull Long admissionVersion,
        @NotNull AdmissionAssessmentDecisionStatus decision,
        @NotBlank @Size(max = 500) String conclusion
) {
}
