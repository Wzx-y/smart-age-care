package care.cloud.care.audit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuditRetentionScheduler {
    private final AuditEventService service;

    public AuditRetentionScheduler(AuditEventService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${audit.retention-scan-ms:86400000}")
    public void purgeExpiredAuditEvents() {
        service.purgeExpired();
    }
}
