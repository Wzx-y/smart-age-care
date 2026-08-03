package care.cloud.care.audit;

import java.time.OffsetDateTime;
import java.util.List;

public interface AuditEventRepository {
    List<AuditEvent> findByTenantId(Long tenantId, String resourceType, String action, Long actorId, int limit);
    void purgeBefore(OffsetDateTime cutoff);
}
