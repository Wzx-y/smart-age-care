package care.cloud.care.resident;

import jakarta.validation.constraints.Size;

public record UpdateResidentHealthProfileRequest(
        @Size(max = 8) String bloodType,
        @Size(max = 1024) String allergySummary,
        @Size(max = 2048) String chronicConditions,
        @Size(max = 2048) String medicationNotes,
        @Size(max = 32) String careLevel,
        @Size(max = 64) String mobilityStatus,
        @Size(max = 64) String cognitionStatus,
        @Size(max = 32) String nutritionRisk,
        @Size(max = 32) String fallRisk,
        @Size(max = 32) String pressureInjuryRisk,
        @Size(max = 32) String infectionRisk,
        @Size(max = 2048) String careNotes
) {
}
