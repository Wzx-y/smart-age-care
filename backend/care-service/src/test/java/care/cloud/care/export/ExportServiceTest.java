package care.cloud.care.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.reporting.OperationalReport;
import care.cloud.care.reporting.OperationalReportRepository;
import care.cloud.care.reporting.OperationalReportService;
import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ExportServiceTest {
    private final ExportRepository repository = Mockito.mock(ExportRepository.class);
    private final OperationalReportRepository reportRepository = Mockito.mock(OperationalReportRepository.class);
    private final OperationalReportService reportService = Mockito.mock(OperationalReportService.class);
    private final ExportFileStorage storage = Mockito.mock(ExportFileStorage.class);
    private final ExportService service = new ExportService(repository, reportRepository, reportService, storage, 24L);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void createUsesTrustedTenantAndAuditsTheQueuedJob() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(repository.create(any())).thenAnswer(invocation -> {
            ExportJob pending = invocation.getArgument(0);
            return new ExportJob(71L, pending.tenantId(), pending.exportType(), pending.periodStart(), pending.periodEnd(), ExportStatus.QUEUED,
                    null, null, pending.createdBy(), pending.createdAt(), null, pending.expiresAt(), 0L);
        });
        CreateExportRequest request = new CreateExportRequest(ExportType.OPERATIONAL_SUMMARY_CSV, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        ExportJob created = service.create(request);

        ArgumentCaptor<ExportJob> captor = ArgumentCaptor.forClass(ExportJob.class);
        verify(reportService).validatePeriod(request.periodStart(), request.periodEnd());
        verify(repository).create(captor.capture());
        assertEquals(12L, captor.getValue().tenantId());
        assertEquals(45L, captor.getValue().createdBy());
        assertEquals(71L, created.id());
        verify(repository).recordAudit(12L, 71L, 45L, "CREATED");
    }

    @Test
    void generatorStoresAggregateOnlyAndMarksJobReady() {
        ExportJob claimed = job(71L, ExportStatus.GENERATING, 1L, OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        when(repository.claimNextQueued()).thenReturn(Optional.of(claimed));
        when(reportRepository.aggregate(12L, claimed.periodStart(), claimed.periodEnd())).thenReturn(report(claimed));
        when(repository.markReady(eq(claimed), any(), any(), any())).thenReturn(true);

        service.generateNext();

        ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
        verify(storage).store(eq("tenant/12/exports/71/operational-summary-2026-07-01-2026-07-31.csv"), content.capture());
        String csv = new String(content.getValue(), java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(csv.contains("resident_name"));
        assertEquals(true, csv.contains("occupancy_rate_percent,83.3"));
        verify(repository).recordAudit(12L, 71L, null, "READY");
    }

    @Test
    void accessSignsOnlyAReadyUnexpiredJobAndAuditsDownload() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        ExportJob ready = job(71L, ExportStatus.READY, 2L, OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        when(repository.findByIdAndTenantId(71L, 12L)).thenReturn(Optional.of(ready));
        ExportAccessTarget target = new ExportAccessTarget("https://private.example.com/signed", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        when(storage.signDownload("tenant/12/exports/71/report.csv")).thenReturn(target);

        assertEquals(target, service.access(71L));
        verify(repository).recordAudit(12L, 71L, 45L, "DOWNLOADED");
    }

    @Test
    void expiredJobCannotReceiveAnAccessUrl() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        ExportJob expired = job(71L, ExportStatus.READY, 2L, OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(repository.findByIdAndTenantId(71L, 12L)).thenReturn(Optional.of(expired));

        assertThrows(ExportConflictException.class, () -> service.access(71L));
    }

    private ExportJob job(Long id, ExportStatus status, long version, OffsetDateTime expiresAt) {
        return new ExportJob(id, 12L, ExportType.OPERATIONAL_SUMMARY_CSV, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), status,
                "report.csv", "tenant/12/exports/71/report.csv", 45L, OffsetDateTime.now(ZoneOffset.UTC), null, expiresAt, version);
    }

    private OperationalReport report(ExportJob job) {
        return new OperationalReport(job.periodStart(), job.periodEnd(), 18L, 24L, 20L, new BigDecimal("83.3"), 4L, 2L,
                70L, 63L, 2L, new BigDecimal("90.0"), 63L, 1L);
    }
}
