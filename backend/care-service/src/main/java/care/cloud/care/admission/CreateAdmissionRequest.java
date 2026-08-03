package care.cloud.care.admission;

import jakarta.validation.constraints.NotNull;

public record CreateAdmissionRequest(@NotNull Long residentId) {
}
