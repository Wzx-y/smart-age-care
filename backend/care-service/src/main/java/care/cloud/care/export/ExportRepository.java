package care.cloud.care.export;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ExportRepository {
    ExportJob create(ExportJob job);
    List<ExportJob> findByTenantId(Long tenantId);
    Optional<ExportJob> findByIdAndTenantId(Long id, Long tenantId);
    Optional<ExportJob> claimNextQueued();
    boolean markReady(ExportJob job, String fileName, String storageKey, OffsetDateTime completedAt);
    boolean markFailed(ExportJob job);
    List<ExportJob> findReadyExpiredBefore(OffsetDateTime now);
    boolean markExpired(ExportJob job);
    void recordAudit(Long tenantId, Long exportJobId, Long actorId, String action);
}
