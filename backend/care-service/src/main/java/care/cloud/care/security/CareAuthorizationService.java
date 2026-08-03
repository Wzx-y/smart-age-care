package care.cloud.care.security;

import care.cloud.care.care.CareNotFoundException;
import care.cloud.care.care.CareTask;
import care.cloud.care.care.CareTaskRepository;
import org.springframework.stereotype.Service;

@Service
public class CareAuthorizationService {
    public static final String PLAN_MANAGE = "care:plan:manage";
    public static final String PLAN_PUBLISH = "care:plan:publish";
    public static final String TASK_MANAGE = "care:task:manage";
    public static final String TASK_EXECUTE = "care:task:execute";
    public static final String FOLLOW_UP_MANAGE = "care:follow-up:manage";
    public static final String HANDOVER_MANAGE = "care:handover:manage";
    public static final String HANDOVER_RECEIVE = "care:handover:receive";
    public static final String MASTER_DATA_MANAGE = "care:master-data:manage";
    public static final String REPORT_VIEW = "care:report:view";
    public static final String EXPORT_MANAGE = "care:export:manage";
    public static final String EXPORT_DOWNLOAD = "care:export:download";
    public static final String NOTIFICATION_VIEW = "care:notification:view";
    public static final String AUDIT_VIEW = "care:audit:view";

    private final TenantMemberDirectory memberDirectory;
    private final CareTaskRepository taskRepository;

    public CareAuthorizationService(TenantMemberDirectory memberDirectory, CareTaskRepository taskRepository) {
        this.memberDirectory = memberDirectory;
        this.taskRepository = taskRepository;
    }

    public void requireCurrent(String permission) {
        TenantPrincipal principal = TenantContext.requireCurrent();
        memberDirectory.requirePermission(principal.tenantId(), principal.userId(), permission);
    }

    public void requireMember(Long memberId, String permission) {
        TenantPrincipal principal = TenantContext.requireCurrent();
        memberDirectory.requirePermission(principal.tenantId(), memberId, permission);
    }

    public void requireAssignedExecutor(Long taskId) {
        TenantPrincipal principal = TenantContext.requireCurrent();
        CareTask task = taskRepository.findByIdAndTenantId(taskId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("护理任务", taskId));
        if (!principal.userId().equals(task.assigneeId())) throw new CareAccessDeniedException();
        memberDirectory.requirePermission(principal.tenantId(), principal.userId(), TASK_EXECUTE);
    }
}
