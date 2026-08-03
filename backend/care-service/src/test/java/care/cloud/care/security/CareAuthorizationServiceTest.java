package care.cloud.care.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.care.CareTask;
import care.cloud.care.care.CareTaskRepository;
import care.cloud.care.care.CareTaskStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CareAuthorizationServiceTest {
    private final TenantMemberDirectory memberDirectory = Mockito.mock(TenantMemberDirectory.class);
    private final CareTaskRepository taskRepository = Mockito.mock(CareTaskRepository.class);
    private final CareAuthorizationService service = new CareAuthorizationService(memberDirectory, taskRepository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void requireCurrentDelegatesTenantScopedPermissionToRuoYiDirectory() {
        TenantContext.set(new TenantPrincipal(45L, 12L));

        service.requireCurrent(CareAuthorizationService.PLAN_MANAGE);

        verify(memberDirectory).requirePermission(12L, 45L, CareAuthorizationService.PLAN_MANAGE);
    }

    @Test
    void assignedExecutorMustMatchCurrentUserBeforeExecutionPermissionIsChecked() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(taskRepository.findByIdAndTenantId(81L, 12L)).thenReturn(Optional.of(task(52L)));

        assertThrows(CareAccessDeniedException.class, () -> service.requireAssignedExecutor(81L));
    }

    @Test
    void assignedExecutorRequiresRuoYiExecutionPermission() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(taskRepository.findByIdAndTenantId(81L, 12L)).thenReturn(Optional.of(task(45L)));

        service.requireAssignedExecutor(81L);

        verify(memberDirectory).requirePermission(12L, 45L, CareAuthorizationService.TASK_EXECUTE);
    }

    private CareTask task(Long assigneeId) {
        return new CareTask(81L, 12L, 71L, 91L, "Morning care", OffsetDateTime.now(ZoneOffset.UTC), assigneeId,
                CareTaskStatus.PENDING, null, 0L);
    }
}
