package care.cloud.care.care;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CareServiceRecordQueryServiceTest {
    private final CareTaskRepository repository = org.mockito.Mockito.mock(CareTaskRepository.class);
    private final CareServiceRecordQueryService service = new CareServiceRecordQueryService(repository);

    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test
    void passesServerSideFiltersWithCurrentTenantOnly() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        LocalDate date = LocalDate.of(2026, 7, 31);
        when(repository.findServiceRecords(12L, 91L, date, 45L)).thenReturn(List.of());
        assertEquals(List.of(), service.list(91L, date, 45L));
        verify(repository).findServiceRecords(12L, 91L, date, 45L);
    }
}
