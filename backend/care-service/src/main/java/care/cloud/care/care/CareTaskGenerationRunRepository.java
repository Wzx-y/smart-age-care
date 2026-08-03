package care.cloud.care.care;

import java.util.Optional;

public interface CareTaskGenerationRunRepository {
    CareTaskGenerationRun create(CareTaskGenerationRun run);
    Optional<CareTaskGenerationRun> findByIdAndTenantId(Long id, Long tenantId);
}
