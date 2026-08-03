package care.cloud.care.care;

import care.cloud.care.security.TenantContext;
import care.cloud.care.notification.NotificationCategory;
import care.cloud.care.notification.NotificationPriority;
import care.cloud.care.notification.NotificationService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareTaskService {
    private final CarePlanRepository carePlanRepository;
    private final CareTaskRepository careTaskRepository;
    private final CareAuditRepository careAuditRepository;
    private final NotificationService notificationService;

    public CareTaskService(CarePlanRepository carePlanRepository, CareTaskRepository careTaskRepository,
                           CareAuditRepository careAuditRepository, NotificationService notificationService) {
        this.carePlanRepository = carePlanRepository;
        this.careTaskRepository = careTaskRepository;
        this.careAuditRepository = careAuditRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public CareTask create(CreateCareTaskRequest request) {
        var principal = TenantContext.requireCurrent();
        CarePlan initialPlan = carePlanRepository.findByIdAndTenantId(request.planId(), principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", request.planId()));
        carePlanRepository.lockResident(principal.tenantId(), initialPlan.residentId());
        CarePlan plan = carePlanRepository.findByIdAndTenantId(request.planId(), principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理计划", request.planId()));
        requireSchedulable(plan, request.scheduledAt().toLocalDate());
        return careTaskRepository.create(new CareTask(
                null, principal.tenantId(), plan.id(), plan.residentId(), request.taskName().trim(), request.scheduledAt(),
                request.assigneeId(), CareTaskStatus.PENDING, null, 0
        ));
    }

    public List<CareTask> list() {
        return careTaskRepository.findByTenantId(TenantContext.requireCurrent().tenantId());
    }

    public CareTask replay(CareWriteResult result) {
        if (!"TASK".equals(result.resourceType())) {
            throw new CareStateConflictException();
        }
        var principal = TenantContext.requireCurrent();
        CareTask task = task(result.resourceId(), principal.tenantId());
        return new CareTask(task.id(), task.tenantId(), task.planId(), task.residentId(), task.taskName(), task.scheduledAt(),
                task.assigneeId(), CareTaskStatus.valueOf(result.status()), result.detail(), result.version());
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CareTask start(Long taskId, StartCareTaskRequest request) {
        var principal = TenantContext.requireCurrent();
        CareTask task = task(taskId, principal.tenantId());
        if (task.version() != request.taskVersion() || !careTaskRepository.start(task, principal.userId())) {
            throw new CareStateConflictException();
        }
        careAuditRepository.recordTaskAction(principal.tenantId(), principal.userId(), "CARE_TASK_STARTED", task);
        return new CareTask(task.id(), task.tenantId(), task.planId(), task.residentId(), task.taskName(), task.scheduledAt(),
                task.assigneeId(), CareTaskStatus.IN_PROGRESS, null, task.version() + 1);
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CareTask complete(Long taskId, CompleteCareTaskRequest request) {
        var principal = TenantContext.requireCurrent();
        CareTask task = task(taskId, principal.tenantId());
        if (task.version() != request.taskVersion()) {
            throw new CareStateConflictException();
        }
        if (!careTaskRepository.complete(task, principal.userId())) {
            throw new CareStateConflictException();
        }
        careTaskRepository.createServiceRecord(principal.tenantId(), task.id(), principal.userId(), request.resultNote().trim());
        careAuditRepository.recordTaskAction(principal.tenantId(), principal.userId(), "CARE_TASK_COMPLETED", task);
        return new CareTask(
                task.id(), task.tenantId(), task.planId(), task.residentId(), task.taskName(), task.scheduledAt(), task.assigneeId(),
                CareTaskStatus.COMPLETED, null, task.version() + 1
        );
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CareTask markException(Long taskId, MarkCareTaskExceptionRequest request) {
        var principal = TenantContext.requireCurrent();
        CareTask task = task(taskId, principal.tenantId());
        String reason = request.exceptionReason().trim();
        if (task.version() != request.taskVersion() || !careTaskRepository.markException(task, reason)) {
            throw new CareStateConflictException();
        }
        careTaskRepository.createExceptionFollowUp(principal.tenantId(), task, principal.userId(), reason);
        careAuditRepository.recordTaskAction(principal.tenantId(), principal.userId(), "CARE_TASK_EXCEPTION", task);
        notificationService.publishBroadcast(principal.tenantId(), NotificationCategory.TASK_EXCEPTION, NotificationPriority.HIGH,
                "护理任务出现异常", "CARE_TASK", task.id(), task.residentId(), "TASK_EXCEPTION:" + task.id());
        return new CareTask(
                task.id(), task.tenantId(), task.planId(), task.residentId(), task.taskName(), task.scheduledAt(), task.assigneeId(),
                CareTaskStatus.EXCEPTION, reason, task.version() + 1
        );
    }

    private CareTask task(Long taskId, Long tenantId) {
        return careTaskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new CareNotFoundException("护理任务", taskId));
    }

    private void requireSchedulable(CarePlan plan, LocalDate scheduledDate) {
        if (plan.status() != CarePlanStatus.PUBLISHED || scheduledDate.isBefore(plan.startDate())
                || (plan.endDate() != null && scheduledDate.isAfter(plan.endDate()))) {
            throw new CareStateConflictException();
        }
    }
}
