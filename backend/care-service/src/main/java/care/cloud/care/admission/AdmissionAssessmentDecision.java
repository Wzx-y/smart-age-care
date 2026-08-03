package care.cloud.care.admission;

import java.time.OffsetDateTime;

public record AdmissionAssessmentDecision(
        Long id,
        Long admissionId,
        AdmissionAssessmentDecisionStatus decision,
        String conclusion,
        Long assessedBy,
        OffsetDateTime assessedAt
) {
}
