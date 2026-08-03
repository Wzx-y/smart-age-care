package care.cloud.care.care;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CareTaskFollowUpServiceTest {
    private final CareTaskFollowUpRepository repository = Mockito.mock(CareTaskFollowUpRepository.class);
    private final CareTaskFollowUpService service = new CareTaskFollowUpService(repository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void resolveUsesCurrentTenantActorAndOptimisticVersion() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CareTaskFollowUp followUp = new CareTaskFollowUp(31L, 12L, 81L, 91L, CareTaskFollowUpStatus.OPEN,
                "长者拒绝服务", null, 45L, OffsetDateTime.now(ZoneOffset.UTC), null, null, 2L);
        when(repository.findByIdAndTenantId(31L, 12L)).thenReturn(Optional.of(followUp));
        when(repository.resolve(followUp, 45L, "已与家属确认后改期")).thenReturn(true);

        CareTaskFollowUp resolved = service.resolve(31L, new ResolveCareTaskFollowUpRequest(2L, "已与家属确认后改期"));

        verify(repository).resolve(followUp, 45L, "已与家属确认后改期");
        assertEquals(CareTaskFollowUpStatus.RESOLVED, resolved.status());
        assertEquals(3L, resolved.version());
    }
}
