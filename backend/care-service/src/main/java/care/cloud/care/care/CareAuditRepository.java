package care.cloud.care.care;

public interface CareAuditRepository {
    void recordTaskAction(Long tenantId, Long actorId, String action, CareTask task);
}
