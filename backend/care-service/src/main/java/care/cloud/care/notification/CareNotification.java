package care.cloud.care.notification;

import java.time.OffsetDateTime;

public record CareNotification(Long id, Long tenantId, Long recipientId, NotificationCategory category,
                               NotificationPriority priority, String title, String resourceType, Long resourceId,
                               Long relatedResidentId, String dedupeKey, OffsetDateTime createdAt, OffsetDateTime readAt) {
}
