package care.cloud.device.telemetry;

import java.util.Map;

/** Vendor-specific adapters normalize payloads before business rules create device events or alerts. */
public interface DeviceTelemetryNormalizer {
    boolean supports(String productCode);

    DeviceTelemetry normalize(String productCode, String deviceCode, Map<String, Object> payload);
}
