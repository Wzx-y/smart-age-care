package care.cloud.device.telemetry;

import java.time.OffsetDateTime;
import java.util.Map;

public record DeviceTelemetry(
        Long tenantId,
        String deviceCode,
        String eventType,
        OffsetDateTime occurredAt,
        Map<String, Object> metrics
) {
}
