package care.cloud.care.care;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import java.time.LocalDate;

public interface CareTaskRepository {
    CareTask create(CareTask task);
    boolean createFromTemplate(CarePlanTaskTemplate template, LocalDate serviceDate);
    Optional<CareTask> findByIdAndTenantId(Long id, Long tenantId);
    List<CareTask> findByIdsAndTenantId(List<Long> ids, Long tenantId);
    List<CareTask> findByTenantId(Long tenantId);
    List<CareTask> findUnfinishedScheduledBefore(OffsetDateTime scheduledBefore);
    boolean start(CareTask task, Long actorId);
    boolean complete(CareTask task, Long actorId);
    boolean markException(CareTask task, String reason);
    void createExceptionFollowUp(Long tenantId, CareTask task, Long actorId, String reason);
    void createServiceRecord(Long tenantId, Long taskId, Long actorId, String resultNote);
    List<CareServiceRecord> findServiceRecords(Long tenantId, Long residentId, LocalDate serviceDate, Long executorId);
}
