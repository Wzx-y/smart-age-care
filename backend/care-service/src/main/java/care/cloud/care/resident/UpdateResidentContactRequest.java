package care.cloud.care.resident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateResidentContactRequest(
        @NotNull Long version,
        @NotBlank(message = "联系人姓名不能为空") String name,
        @NotBlank(message = "与长者关系不能为空") String relationshipText,
        @Pattern(regexp = "^[0-9+() -]{6,24}$", message = "联系电话格式不正确") String phone,
        boolean primaryContact
) {
}
