package care.cloud.care.admission;

import jakarta.validation.constraints.NotNull;

public record CancelAdmissionRequest(@NotNull Long admissionVersion, Long bedVersion) {
}
