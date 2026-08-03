package care.cloud.care.resident;

import java.time.OffsetDateTime;

public record ResidentHealthProfile(
        Long residentId, String bloodType, String allergySummary, String chronicConditions, String medicationNotes,
        String careLevel, String mobilityStatus, String cognitionStatus, String nutritionRisk, String fallRisk,
        String pressureInjuryRisk, String infectionRisk, String careNotes, Long updatedBy, OffsetDateTime updatedAt
) {
}
