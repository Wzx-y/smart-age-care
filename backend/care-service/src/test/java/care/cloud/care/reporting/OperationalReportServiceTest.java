package care.cloud.care.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OperationalReportServiceTest {
    private final OperationalReportRepository repository = Mockito.mock(OperationalReportRepository.class);
    private final OperationalReportService service = new OperationalReportService(repository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void reportAggregatesOnlyTheCurrentTenant() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        OperationalReport report = new OperationalReport(from, to, 18L, 24L, 20L, new BigDecimal("83.3"), 4L, 2L,
                70L, 63L, 2L, new BigDecimal("90.0"), 63L, 1L);
        when(repository.aggregate(12L, from, to)).thenReturn(report);

        assertEquals(report, service.report(from, to));
        verify(repository).aggregate(12L, from, to);
    }

    @Test
    void reportRejectsInvertedOrOverlongPeriods() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validatePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 31)));
        assertThrows(IllegalArgumentException.class,
                () -> service.validatePeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 3)));
    }
}
