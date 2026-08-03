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
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CarePlanServiceTest {
    private final CarePlanRepository carePlanRepository = Mockito.mock(CarePlanRepository.class);
    private final CarePlanService service = new CarePlanService(carePlanRepository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void createLocksResidentAndUsesTenantScopedNextVersion() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(carePlanRepository.nextVersion(12L, 91L)).thenReturn(3L);
        when(carePlanRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CarePlan created = service.create(new CreateCarePlanRequest(
                91L, "晨间照护", "每日 08:00", LocalDate.of(2026, 8, 1), null, null
        ));

        ArgumentCaptor<CarePlan> captor = ArgumentCaptor.forClass(CarePlan.class);
        verify(carePlanRepository).lockResident(12L, 91L);
        verify(carePlanRepository).create(captor.capture());
        assertEquals(12L, created.tenantId());
        assertEquals(45L, created.ownerId());
        assertEquals(3L, captor.getValue().version());
        assertEquals(CarePlanStatus.DRAFT, captor.getValue().status());
    }

    @Test
    void publishRejectsStalePlanVersionBeforeChangingExistingPublishedPlan() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CarePlan plan = new CarePlan(71L, 12L, 91L, null, "晨间照护", "每日 08:00", 45L,
                CarePlanStatus.DRAFT, 3L, LocalDate.of(2026, 8, 1), null, null,
                OffsetDateTime.now(ZoneOffset.UTC));
        when(carePlanRepository.findByIdAndTenantId(71L, 12L)).thenReturn(Optional.of(plan));

        assertThrows(CareStateConflictException.class, () -> service.publish(71L, new PublishCarePlanRequest(2L)));

        verify(carePlanRepository).lockResident(12L, 91L);
        verify(carePlanRepository, Mockito.never()).supersedePublished(any(), any(), any());
    }

    @Test
    void createsAPlanOnlyWhenItsAssessmentBelongsToTheSameResidentAndTenant() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(carePlanRepository.assessmentExists(12L, 91L, 31L)).thenReturn(true);
        when(carePlanRepository.nextVersion(12L, 91L)).thenReturn(4L);
        when(carePlanRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CarePlan created = service.create(new CreateCarePlanRequest(91L, "风险照护", "每日", LocalDate.of(2026, 8, 1), null, 31L));

        assertEquals(31L, created.assessmentId());
        verify(carePlanRepository).assessmentExists(12L, 91L, 31L);
    }
}
