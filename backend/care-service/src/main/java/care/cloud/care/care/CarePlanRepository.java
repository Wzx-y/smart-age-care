package care.cloud.care.care;

import java.util.List;
import java.util.Optional;

public interface CarePlanRepository {
    CarePlan create(CarePlan plan);
    Optional<CarePlan> findByIdAndTenantId(Long id, Long tenantId);
    List<CarePlan> findByTenantId(Long tenantId);
    void lockResident(Long tenantId, Long residentId);
    boolean assessmentExists(Long tenantId, Long residentId, Long assessmentId);
    long nextVersion(Long tenantId, Long residentId);
    boolean publish(CarePlan plan);
    boolean updateDraft(CarePlan plan);
    boolean cancel(CarePlan plan);
    void supersedePublished(Long tenantId, Long residentId, Long exceptPlanId);
}
