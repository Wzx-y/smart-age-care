package care.cloud.care.notification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CareNotificationRepository {
    void createIfAbsent(CareNotification notification);
    List<CareNotification> findVisible(Long tenantId, Long recipientId, boolean unreadOnly);
    Optional<CareNotification> findVisibleById(Long tenantId, Long recipientId, Long notificationId);
    boolean markPersonalRead(Long tenantId, Long recipientId, Long notificationId, OffsetDateTime readAt);
    void markBroadcastRead(Long tenantId, Long recipientId, Long notificationId, OffsetDateTime readAt);
    int markAllRead(Long tenantId, Long recipientId, OffsetDateTime readAt);
}
