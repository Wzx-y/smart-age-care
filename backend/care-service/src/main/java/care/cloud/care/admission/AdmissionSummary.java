package care.cloud.care.admission;

import java.time.OffsetDateTime;

public record AdmissionSummary(
        Long id,
        Long residentId,
        String residentName,
        Long bedId,
        String roomNo,
        String bedNo,
        AdmissionStatus status,
        long version,
        Long bedVersion,
        OffsetDateTime reservedUntil,
        OffsetDateTime appliedAt,
        AdmissionAssessmentDecisionStatus assessmentDecision,
        String assessmentConclusion,
        OffsetDateTime assessedAt
) {
}
