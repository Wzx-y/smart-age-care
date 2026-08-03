package care.cloud.care.care;

import care.cloud.care.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarePlanTaskTemplateService {
    private final CarePlanRepository carePlanRepository;
    private final CarePlanTaskTemplateRepository repository;

    public CarePlanTaskTemplateService(CarePlanRepository carePlanRepository, CarePlanTaskTemplateRepository repository) {
        this.carePlanRepository = carePlanRepository;
        this.repository = repository;
    }

    @Transactional
    public CarePlanTaskTemplate create(CreateCarePlanTaskTemplateRequest request) {
        var principal = TenantContext.requireCurrent();
        CarePlan initialPlan = carePlanRepository.findByIdAndTenantId(request.planId(), principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", request.planId()));
        carePlanRepository.lockResident(principal.tenantId(), initialPlan.residentId());
        CarePlan plan = carePlanRepository.findByIdAndTenantId(request.planId(), principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", request.planId()));
        if (plan.status() != CarePlanStatus.DRAFT && plan.status() != CarePlanStatus.PUBLISHED) {
            throw new CareStateConflictException();
        }
        return repository.create(new CarePlanTaskTemplate(null, principal.tenantId(), plan.id(), plan.residentId(), request.taskName().trim(),
                request.scheduledTime(), request.assigneeId(), true, 0L), principal.userId());
    }

    public CarePlanTaskTemplate replay(CareWriteResult result) {
        if (!"TEMPLATE".equals(result.resourceType())) throw new CareStateConflictException();
        var principal = TenantContext.requireCurrent();
        return repository.findByIdAndTenantId(result.resourceId(), principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理任务模板", result.resourceId()));
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CarePlanTaskTemplate update(Long templateId, UpdateCarePlanTaskTemplateRequest request) {
        var principal = TenantContext.requireCurrent();
        CarePlanTaskTemplate template = repository.findByIdAndTenantId(templateId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理任务模板", templateId));
        if (!template.active() || template.version() != request.templateVersion()) throw new CareStateConflictException();
        CarePlanTaskTemplate updated = new CarePlanTaskTemplate(template.id(), template.tenantId(), template.planId(), template.residentId(),
                request.taskName().trim(), request.scheduledTime(), request.assigneeId(), true, template.version());
        if (!repository.update(updated)) throw new CareStateConflictException();
        return new CarePlanTaskTemplate(updated.id(), updated.tenantId(), updated.planId(), updated.residentId(), updated.taskName(),
                updated.scheduledTime(), updated.assigneeId(), true, updated.version() + 1);
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CarePlanTaskTemplate deactivate(Long templateId, ChangeCarePlanTaskTemplateStatusRequest request) {
        var principal = TenantContext.requireCurrent();
        CarePlanTaskTemplate template = repository.findByIdAndTenantId(templateId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理任务模板", templateId));
        if (!template.active() || template.version() != request.templateVersion() || !repository.deactivate(template)) throw new CareStateConflictException();
        return new CarePlanTaskTemplate(template.id(), template.tenantId(), template.planId(), template.residentId(), template.taskName(),
                template.scheduledTime(), template.assigneeId(), false, template.version() + 1);
    }

    public List<CarePlanTaskTemplate> list(Long planId) {
        var principal = TenantContext.requireCurrent();
        carePlanRepository.findByIdAndTenantId(planId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", planId));
        return repository.findByPlanIdAndTenantId(planId, principal.tenantId());
    }
}
