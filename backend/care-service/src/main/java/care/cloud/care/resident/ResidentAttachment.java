package care.cloud.care.resident;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;

public record ResidentAttachment(
        Long id,
        Long residentId,
        String fileName,
        String contentType,
        long byteSize,
        @JsonIgnore String storageKey,
        String uploadStatus,
        OffsetDateTime createdAt,
        OffsetDateTime uploadedAt
) {
}
