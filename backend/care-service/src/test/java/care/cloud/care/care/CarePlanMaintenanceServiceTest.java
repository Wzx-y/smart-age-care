package care.cloud.care.care;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CarePlanMaintenanceServiceTest {
    private final CarePlanRepository repository = Mockito.mock(CarePlanRepository.class);
    private final CarePlanService service = new CarePlanService(repository);

    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test
    void updatesOnlyCurrentTenantDraftPlanWithMatchingVersion() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CarePlan plan = plan(CarePlanStatus.DRAFT, 3L);
        when(repository.findByIdAndTenantId(71L, 12L)).thenReturn(Optional.of(plan));
        when(repository.updateDraft(any())).thenReturn(true);
        CarePlan updated = service.update(71L, new UpdateCarePlanRequest(3L, "晚间照护", "每日 20:00", LocalDate.of(2026, 8, 1), null, null));
        assertEquals("晚间照护", updated.planName());
        assertEquals(4L, updated.version());
        verify(repository).updateDraft(any());
    }

    @Test
    void rejectsEditingPublishedPlanOrStaleVersion() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(repository.findByIdAndTenantId(71L, 12L)).thenReturn(Optional.of(plan(CarePlanStatus.PUBLISHED, 3L)));
        assertThrows(CareStateConflictException.class,
                () -> service.update(71L, new UpdateCarePlanRequest(3L, "修改", "每日", LocalDate.now(), null, null)));
    }

    @Test
    void cancelsPublishedPlanWithOptimisticLock() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CarePlan plan = plan(CarePlanStatus.PUBLISHED, 3L);
        when(repository.findByIdAndTenantId(71L, 12L)).thenReturn(Optional.of(plan));
        when(repository.cancel(plan)).thenReturn(true);
        CarePlan cancelled = service.cancel(71L, new ChangeCarePlanStatusRequest(3L));
        assertEquals(CarePlanStatus.CANCELLED, cancelled.status());
        assertEquals(4L, cancelled.version());
    }

    private CarePlan plan(CarePlanStatus status, long version) {
        return new CarePlan(71L, 12L, 91L, null, "晨间照护", "每日 08:00", 45L, status, version,
                LocalDate.of(2026, 8, 1), null, null, OffsetDateTime.parse("2026-07-31T08:00:00Z"));
    }
}
