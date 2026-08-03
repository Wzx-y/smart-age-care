package care.cloud.care.care;

import care.cloud.care.security.TenantContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareTaskGenerationService {
    private final CarePlanTaskTemplateRepository templateRepository;
    private final CareTaskRepository taskRepository;
    private final CareTaskGenerationRunRepository runRepository;

    public CareTaskGenerationService(
            CarePlanTaskTemplateRepository templateRepository, CareTaskRepository taskRepository, CareTaskGenerationRunRepository runRepository
    ) {
        this.templateRepository = templateRepository;
        this.taskRepository = taskRepository;
        this.runRepository = runRepository;
    }

    @Transactional
    public CareTaskGenerationRun generate(GenerateCareTasksRequest request) {
        var principal = TenantContext.requireCurrent();
        int created = 0;
        for (CarePlanTaskTemplate template : templateRepository.findActiveForPublishedPlans(principal.tenantId(), request.serviceDate())) {
            if (taskRepository.createFromTemplate(template, request.serviceDate())) created++;
        }
        return runRepository.create(new CareTaskGenerationRun(null, principal.tenantId(), request.serviceDate(), created,
                principal.userId(), OffsetDateTime.now(ZoneOffset.UTC)));
    }

    public CareTaskGenerationRun replay(CareWriteResult result) {
        if (!"TASK_GENERATION_RUN".equals(result.resourceType())) throw new CareStateConflictException();
        return runRepository.findByIdAndTenantId(result.resourceId(), TenantContext.requireCurrent().tenantId())
                .orElseThrow(() -> new CareNotFoundException("任务生成批次", result.resourceId()));
    }
}
