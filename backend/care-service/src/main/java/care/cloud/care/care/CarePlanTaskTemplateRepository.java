package care.cloud.care.care;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CarePlanTaskTemplateRepository {
    CarePlanTaskTemplate create(CarePlanTaskTemplate template, Long actorId);

    Optional<CarePlanTaskTemplate> findByIdAndTenantId(Long id, Long tenantId);

    List<CarePlanTaskTemplate> findByPlanIdAndTenantId(Long planId, Long tenantId);

    List<CarePlanTaskTemplate> findActiveForPublishedPlans(Long tenantId, LocalDate serviceDate);

    boolean update(CarePlanTaskTemplate template);

    boolean deactivate(CarePlanTaskTemplate template);
}
