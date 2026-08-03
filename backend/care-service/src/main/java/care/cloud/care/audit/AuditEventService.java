package care.cloud.care.audit;

import care.cloud.care.security.TenantContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuditEventService {
    private final AuditEventRepository repository;
    private final long retentionDays;

    public AuditEventService(AuditEventRepository repository, @Value("${audit.retention-days:2555}") long retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
    }

    public List<AuditEvent> list(String resourceType, String action, Long actorId, int limit) {
        var principal = TenantContext.requireCurrent();
        return repository.findByTenantId(principal.tenantId(), blankToNull(resourceType), blankToNull(action), actorId, Math.min(Math.max(limit, 1), 100));
    }

    public void purgeExpired() {
        repository.purgeBefore(OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
