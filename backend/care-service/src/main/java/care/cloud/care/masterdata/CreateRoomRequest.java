package care.cloud.care.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank @Size(max = 64) String building,
        @NotBlank @Size(max = 32) String floor,
        @NotBlank @Size(max = 64) String roomNo,
        @NotBlank @Size(max = 64) String roomType,
        @NotBlank @Size(max = 64) String nursingUnit
) {
}
