package care.cloud.care.resident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record UpdateResidentRequest(
        @NotBlank(message = "长者姓名不能为空") String name,
        String gender,
        LocalDate birthDate,
        @NotBlank(message = "紧急联系人不能为空") String emergencyContactName,
        @Pattern(regexp = "^[0-9+() -]{6,24}$", message = "紧急联系电话格式不正确") String emergencyContactPhone
) {
}
