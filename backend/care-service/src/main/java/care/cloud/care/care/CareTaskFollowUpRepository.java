package care.cloud.care.care;

import java.util.List;
import java.util.Optional;

public interface CareTaskFollowUpRepository {
    Optional<CareTaskFollowUp> findByIdAndTenantId(Long id, Long tenantId);

    List<CareTaskFollowUp> findByTenantId(Long tenantId, CareTaskFollowUpStatus status);

    long countOpenByTaskIdsAndTenantId(List<Long> taskIds, Long tenantId);

    boolean resolve(CareTaskFollowUp followUp, Long actorId, String resolutionNote);
}
