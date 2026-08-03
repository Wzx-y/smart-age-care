package care.cloud.care.care;

import care.cloud.care.security.TenantContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarePlanService {
    private final CarePlanRepository carePlanRepository;

    public CarePlanService(CarePlanRepository carePlanRepository) {
        this.carePlanRepository = carePlanRepository;
    }

    @Transactional
    public CarePlan create(CreateCarePlanRequest request) {
        var principal = TenantContext.requireCurrent();
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("计划结束日期不能早于开始日期");
        }
        carePlanRepository.lockResident(principal.tenantId(), request.residentId());
        if (request.assessmentId() != null && !carePlanRepository.assessmentExists(principal.tenantId(), request.residentId(), request.assessmentId())) {
            throw new CareNotFoundException("评估记录", request.assessmentId());
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return carePlanRepository.create(new CarePlan(
                null, principal.tenantId(), request.residentId(), request.assessmentId(), request.planName().trim(), request.frequencyText().trim(),
                principal.userId(), CarePlanStatus.DRAFT, carePlanRepository.nextVersion(principal.tenantId(), request.residentId()),
                request.startDate(), request.endDate(), null, now
        ));
    }

    public List<CarePlan> list() {
        return carePlanRepository.findByTenantId(TenantContext.requireCurrent().tenantId());
    }

    public CarePlan replay(CareWriteResult result) {
        if (!"PLAN".equals(result.resourceType())) {
            throw new CareStateConflictException();
        }
        var principal = TenantContext.requireCurrent();
        CarePlan plan = carePlanRepository.findByIdAndTenantId(result.resourceId(), principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", result.resourceId()));
        return new CarePlan(plan.id(), plan.tenantId(), plan.residentId(), plan.assessmentId(), plan.planName(), plan.frequencyText(), plan.ownerId(),
                CarePlanStatus.valueOf(result.status()), result.version(), plan.startDate(), plan.endDate(), plan.publishedAt(), plan.createdAt());
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CarePlan update(Long planId, UpdateCarePlanRequest request) {
        var principal = TenantContext.requireCurrent();
        CarePlan plan = carePlanRepository.findByIdAndTenantId(planId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", planId));
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) throw new CareStateConflictException();
        if (plan.version() != request.planVersion() || plan.status() != CarePlanStatus.DRAFT) throw new CareStateConflictException();
        if (request.assessmentId() != null && !carePlanRepository.assessmentExists(principal.tenantId(), plan.residentId(), request.assessmentId())) {
            throw new CareNotFoundException("评估记录", request.assessmentId());
        }
        CarePlan updated = new CarePlan(plan.id(), plan.tenantId(), plan.residentId(), request.assessmentId(), request.planName().trim(), request.frequencyText().trim(),
                plan.ownerId(), plan.status(), plan.version(), request.startDate(), request.endDate(), plan.publishedAt(), plan.createdAt());
        if (!carePlanRepository.updateDraft(updated)) throw new CareStateConflictException();
        return new CarePlan(updated.id(), updated.tenantId(), updated.residentId(), updated.assessmentId(), updated.planName(), updated.frequencyText(), updated.ownerId(),
                updated.status(), updated.version() + 1, updated.startDate(), updated.endDate(), updated.publishedAt(), updated.createdAt());
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CarePlan cancel(Long planId, ChangeCarePlanStatusRequest request) {
        var principal = TenantContext.requireCurrent();
        CarePlan plan = carePlanRepository.findByIdAndTenantId(planId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", planId));
        if (plan.version() != request.planVersion() || !carePlanRepository.cancel(plan)) throw new CareStateConflictException();
        return new CarePlan(plan.id(), plan.tenantId(), plan.residentId(), plan.assessmentId(), plan.planName(), plan.frequencyText(), plan.ownerId(),
                CarePlanStatus.CANCELLED, plan.version() + 1, plan.startDate(), plan.endDate(), plan.publishedAt(), plan.createdAt());
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CarePlan publish(Long planId, PublishCarePlanRequest request) {
        var principal = TenantContext.requireCurrent();
        CarePlan initiallyFound = carePlanRepository.findByIdAndTenantId(planId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", planId));
        carePlanRepository.lockResident(principal.tenantId(), initiallyFound.residentId());
        CarePlan plan = carePlanRepository.findByIdAndTenantId(planId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", planId));
        if (plan.version() != request.planVersion() || plan.status() != CarePlanStatus.DRAFT) {
            throw new CareStateConflictException();
        }
        carePlanRepository.supersedePublished(principal.tenantId(), plan.residentId(), plan.id());
        if (!carePlanRepository.publish(plan)) {
            throw new CareStateConflictException();
        }
        return new CarePlan(
                plan.id(), plan.tenantId(), plan.residentId(), plan.assessmentId(), plan.planName(), plan.frequencyText(), plan.ownerId(),
                CarePlanStatus.PUBLISHED, plan.version(), plan.startDate(), plan.endDate(), OffsetDateTime.now(ZoneOffset.UTC), plan.createdAt()
        );
    }
}
