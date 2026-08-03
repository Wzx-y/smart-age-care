package care.cloud.care.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMasterDataItemRequest(
        @NotBlank @Size(max = 64) String itemCode,
        @NotBlank @Size(max = 128) String itemName,
        @Size(max = 500) String description,
        @NotNull Integer sortOrder
) {
}
