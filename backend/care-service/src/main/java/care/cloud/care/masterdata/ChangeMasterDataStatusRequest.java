package care.cloud.care.masterdata;

import jakarta.validation.constraints.NotNull;

public record ChangeMasterDataStatusRequest(@NotNull Long version, boolean enabled) {
}
