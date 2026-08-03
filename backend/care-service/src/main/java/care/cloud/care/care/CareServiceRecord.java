package care.cloud.care.care;

import java.time.OffsetDateTime;

public record CareServiceRecord(Long id, Long taskId, Long residentId, Long executorId, String resultNote, OffsetDateTime completedAt) {
}
