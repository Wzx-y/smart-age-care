package care.cloud.care.care;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record CareShiftHandover(
        Long id,
        Long tenantId,
        LocalDate shiftDate,
        String shiftCode,
        Long fromUserId,
        Long toUserId,
        String note,
        CareShiftHandoverStatus status,
        OffsetDateTime submittedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long version,
        List<Long> taskIds
) {
}
