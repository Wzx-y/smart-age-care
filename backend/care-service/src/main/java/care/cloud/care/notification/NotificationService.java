package care.cloud.care.notification;

import care.cloud.care.security.TenantContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    public static final long ORGANIZATION_BROADCAST = 0L;
    private final CareNotificationRepository repository;

    public NotificationService(CareNotificationRepository repository) {
        this.repository = repository;
    }

    public void publishBroadcast(Long tenantId, NotificationCategory category, NotificationPriority priority, String title,
                                 String resourceType, Long resourceId, Long residentId, String dedupeKey) {
        repository.createIfAbsent(new CareNotification(null, tenantId, ORGANIZATION_BROADCAST, category, priority, title,
                resourceType, resourceId, residentId, dedupeKey, OffsetDateTime.now(ZoneOffset.UTC), null));
    }

    public void publishToMember(Long tenantId, Long recipientId, NotificationCategory category, NotificationPriority priority,
                                String title, String resourceType, Long resourceId, Long residentId, String dedupeKey) {
        repository.createIfAbsent(new CareNotification(null, tenantId, recipientId, category, priority, title,
                resourceType, resourceId, residentId, dedupeKey, OffsetDateTime.now(ZoneOffset.UTC), null));
    }

    @Transactional(readOnly = true)
    public List<CareNotification> list(boolean unreadOnly) {
        var principal = TenantContext.requireCurrent();
        return repository.findVisible(principal.tenantId(), principal.userId(), unreadOnly);
    }

    @Transactional
    public void markRead(Long notificationId) {
        var principal = TenantContext.requireCurrent();
        CareNotification notification = repository.findVisibleById(principal.tenantId(), principal.userId(), notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (notification.recipientId().equals(ORGANIZATION_BROADCAST)) {
            repository.markBroadcastRead(principal.tenantId(), principal.userId(), notificationId, OffsetDateTime.now(ZoneOffset.UTC));
        } else {
            repository.markPersonalRead(principal.tenantId(), principal.userId(), notificationId, OffsetDateTime.now(ZoneOffset.UTC));
        }
    }

    @Transactional
    public int markAllRead() {
        var principal = TenantContext.requireCurrent();
        return repository.markAllRead(principal.tenantId(), principal.userId(), OffsetDateTime.now(ZoneOffset.UTC));
    }
}
