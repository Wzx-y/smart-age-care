package care.cloud.care.audit;

import static org.mockito.Mockito.verify;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuditEventServiceTest {
    private final AuditEventRepository repository = Mockito.mock(AuditEventRepository.class);
    private final AuditEventService service = new AuditEventService(repository, 2555L);

    @AfterEach
    void clearTenantContext() { TenantContext.clear(); }

    @Test
    void listScopesFiltersAndLimitToCurrentTenant() {
        TenantContext.set(new TenantPrincipal(45L, 12L));

        service.list(" CARE_TASK ", " CARE_TASK_COMPLETED ", 45L, 500);

        verify(repository).findByTenantId(12L, "CARE_TASK", "CARE_TASK_COMPLETED", 45L, 100);
    }
}
