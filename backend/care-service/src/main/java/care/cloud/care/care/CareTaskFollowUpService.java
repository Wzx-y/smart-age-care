package care.cloud.care.care;

import care.cloud.care.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareTaskFollowUpService {
    private final CareTaskFollowUpRepository repository;

    public CareTaskFollowUpService(CareTaskFollowUpRepository repository) {
        this.repository = repository;
    }

    public List<CareTaskFollowUp> list(CareTaskFollowUpStatus status) {
        return repository.findByTenantId(TenantContext.requireCurrent().tenantId(), status);
    }

    public CareTaskFollowUp replay(CareWriteResult result) {
        if (!"FOLLOW_UP".equals(result.resourceType())) {
            throw new CareStateConflictException();
        }
        var principal = TenantContext.requireCurrent();
        CareTaskFollowUp followUp = repository.findByIdAndTenantId(result.resourceId(), principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("异常跟进项", result.resourceId()));
        return new CareTaskFollowUp(followUp.id(), followUp.tenantId(), followUp.taskId(), followUp.residentId(),
                CareTaskFollowUpStatus.valueOf(result.status()), followUp.exceptionReason(), result.detail(), followUp.createdBy(),
                followUp.createdAt(), followUp.closedBy(), followUp.closedAt(), result.version());
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CareTaskFollowUp resolve(Long followUpId, ResolveCareTaskFollowUpRequest request) {
        var principal = TenantContext.requireCurrent();
        CareTaskFollowUp followUp = repository.findByIdAndTenantId(followUpId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("异常跟进项", followUpId));
        if (followUp.version() != request.followUpVersion() || !repository.resolve(followUp, principal.userId(), request.resolutionNote().trim())) {
            throw new CareStateConflictException();
        }
        return new CareTaskFollowUp(followUp.id(), followUp.tenantId(), followUp.taskId(), followUp.residentId(),
                CareTaskFollowUpStatus.RESOLVED, followUp.exceptionReason(), request.resolutionNote().trim(), followUp.createdBy(),
                followUp.createdAt(), principal.userId(), java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC), followUp.version() + 1);
    }
}
