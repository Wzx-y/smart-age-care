package care.cloud.care.notification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareNotificationRepository implements CareNotificationRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCareNotificationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createIfAbsent(CareNotification notification) {
        jdbcTemplate.update("""
                insert ignore into care_notification (tenant_id, recipient_id, category, priority, title, resource_type,
                    resource_id, related_resident_id, dedupe_key, created_at)
                values (:tenantId, :recipientId, :category, :priority, :title, :resourceType, :resourceId,
                    :relatedResidentId, :dedupeKey, :createdAt)
                """, params(notification));
    }

    @Override
    public List<CareNotification> findVisible(Long tenantId, Long recipientId, boolean unreadOnly) {
        String unreadClause = unreadOnly ? " and coalesce(notification.read_at, receipt.read_at) is null" : "";
        return jdbcTemplate.query(("""
                select notification.id, notification.tenant_id, notification.recipient_id, notification.category, notification.priority,
                       notification.title, notification.resource_type, notification.resource_id, notification.related_resident_id,
                       notification.dedupe_key, notification.created_at, coalesce(notification.read_at, receipt.read_at) as visible_read_at
                from care_notification notification left join care_notification_read receipt
                    on receipt.tenant_id = notification.tenant_id and receipt.notification_id = notification.id and receipt.recipient_id = :recipientId
                where notification.tenant_id = :tenantId and notification.recipient_id in (0, :recipientId)%s
                order by coalesce(notification.read_at, receipt.read_at) is null desc, notification.created_at desc, notification.id desc limit 100
                """).formatted(unreadClause), params(tenantId, recipientId), (row, index) -> map(row));
    }

    @Override
    public Optional<CareNotification> findVisibleById(Long tenantId, Long recipientId, Long notificationId) {
        return jdbcTemplate.query("""
                select notification.id, notification.tenant_id, notification.recipient_id, notification.category, notification.priority,
                       notification.title, notification.resource_type, notification.resource_id, notification.related_resident_id,
                       notification.dedupe_key, notification.created_at, coalesce(notification.read_at, receipt.read_at) as visible_read_at
                from care_notification notification left join care_notification_read receipt
                    on receipt.tenant_id = notification.tenant_id and receipt.notification_id = notification.id and receipt.recipient_id = :recipientId
                where notification.id = :notificationId and notification.tenant_id = :tenantId and notification.recipient_id in (0, :recipientId)
                """, params(tenantId, recipientId).addValue("notificationId", notificationId), (row, index) -> map(row)).stream().findFirst();
    }

    @Override
    public boolean markPersonalRead(Long tenantId, Long recipientId, Long notificationId, OffsetDateTime readAt) {
        return jdbcTemplate.update("""
                update care_notification set read_at = :readAt
                where id = :notificationId and tenant_id = :tenantId and recipient_id = :recipientId and read_at is null
                """, params(tenantId, recipientId).addValue("notificationId", notificationId).addValue("readAt", readAt)) == 1;
    }

    @Override
    public void markBroadcastRead(Long tenantId, Long recipientId, Long notificationId, OffsetDateTime readAt) {
        jdbcTemplate.update("""
                insert ignore into care_notification_read (tenant_id, notification_id, recipient_id, read_at)
                values (:tenantId, :notificationId, :recipientId, :readAt)
                """, params(tenantId, recipientId).addValue("notificationId", notificationId).addValue("readAt", readAt));
    }

    @Override
    public int markAllRead(Long tenantId, Long recipientId, OffsetDateTime readAt) {
        int personalRead = jdbcTemplate.update("""
                update care_notification set read_at = :readAt
                where tenant_id = :tenantId and recipient_id = :recipientId and read_at is null
                """, params(tenantId, recipientId).addValue("readAt", readAt));
        int broadcastRead = jdbcTemplate.update("""
                insert ignore into care_notification_read (tenant_id, notification_id, recipient_id, read_at)
                select tenant_id, id, :recipientId, :readAt from care_notification
                where tenant_id = :tenantId and recipient_id = 0
                """, params(tenantId, recipientId).addValue("readAt", readAt));
        return personalRead + broadcastRead;
    }

    private CareNotification map(java.sql.ResultSet row) throws java.sql.SQLException {
        java.sql.Timestamp readAt = row.getTimestamp("visible_read_at");
        return new CareNotification(row.getLong("id"), row.getLong("tenant_id"), row.getLong("recipient_id"),
                NotificationCategory.valueOf(row.getString("category")), NotificationPriority.valueOf(row.getString("priority")),
                row.getString("title"), row.getString("resource_type"), row.getLong("resource_id"),
                (Long) row.getObject("related_resident_id"), row.getString("dedupe_key"),
                row.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC), readAt == null ? null : readAt.toInstant().atOffset(ZoneOffset.UTC));
    }

    private MapSqlParameterSource params(Long tenantId, Long recipientId) {
        return new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("recipientId", recipientId);
    }

    private MapSqlParameterSource params(CareNotification notification) {
        return params(notification.tenantId(), notification.recipientId()).addValue("category", notification.category().name())
                .addValue("priority", notification.priority().name()).addValue("title", notification.title())
                .addValue("resourceType", notification.resourceType()).addValue("resourceId", notification.resourceId())
                .addValue("relatedResidentId", notification.relatedResidentId()).addValue("dedupeKey", notification.dedupeKey())
                .addValue("createdAt", notification.createdAt());
    }
}
