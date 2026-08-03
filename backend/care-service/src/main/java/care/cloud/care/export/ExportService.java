package care.cloud.care.export;

import care.cloud.care.reporting.OperationalReport;
import care.cloud.care.reporting.OperationalReportRepository;
import care.cloud.care.reporting.OperationalReportService;
import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {
    private final ExportRepository repository;
    private final OperationalReportRepository reportRepository;
    private final OperationalReportService reportService;
    private final ExportFileStorage storage;
    private final long downloadTtlHours;

    public ExportService(ExportRepository repository, OperationalReportRepository reportRepository, OperationalReportService reportService,
                         ExportFileStorage storage, @Value("${exports.download-ttl-hours:24}") long downloadTtlHours) {
        this.repository = repository;
        this.reportRepository = reportRepository;
        this.reportService = reportService;
        this.storage = storage;
        this.downloadTtlHours = downloadTtlHours;
    }

    @Transactional
    public ExportJob create(CreateExportRequest request) {
        reportService.validatePeriod(request.periodStart(), request.periodEnd());
        TenantPrincipal principal = TenantContext.requireCurrent();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ExportJob created = repository.create(new ExportJob(null, principal.tenantId(), request.exportType(), request.periodStart(),
                request.periodEnd(), ExportStatus.QUEUED, null, null, principal.userId(), now, null, now.plusHours(downloadTtlHours), 0L));
        repository.recordAudit(principal.tenantId(), created.id(), principal.userId(), "CREATED");
        return created;
    }

    @Transactional(readOnly = true)
    public List<ExportJob> list() {
        return repository.findByTenantId(TenantContext.requireCurrent().tenantId());
    }

    @Transactional
    public ExportAccessTarget access(Long exportId) {
        TenantPrincipal principal = TenantContext.requireCurrent();
        ExportJob job = repository.findByIdAndTenantId(exportId, principal.tenantId()).orElseThrow(() -> new ExportNotFoundException(exportId));
        if (job.status() != ExportStatus.READY || !job.expiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)) || job.storageKey() == null) {
            throw new ExportConflictException("Export is not available for download");
        }
        ExportAccessTarget accessTarget = storage.signDownload(job.storageKey());
        repository.recordAudit(principal.tenantId(), job.id(), principal.userId(), "DOWNLOADED");
        return accessTarget;
    }

    public void generateNext() {
        repository.claimNextQueued().ifPresent(this::generate);
    }

    public void expireReadyExports() {
        for (ExportJob job : repository.findReadyExpiredBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            if (repository.markExpired(job)) repository.recordAudit(job.tenantId(), job.id(), null, "EXPIRED");
        }
    }

    private void generate(ExportJob job) {
        try {
            OperationalReport report = reportRepository.aggregate(job.tenantId(), job.periodStart(), job.periodEnd());
            String fileName = "operational-summary-" + job.periodStart() + "-" + job.periodEnd() + ".csv";
            String storageKey = "tenant/" + job.tenantId() + "/exports/" + job.id() + "/" + fileName;
            storage.store(storageKey, csv(report).getBytes(StandardCharsets.UTF_8));
            if (repository.markReady(job, fileName, storageKey, OffsetDateTime.now(ZoneOffset.UTC))) {
                repository.recordAudit(job.tenantId(), job.id(), null, "READY");
            }
        } catch (RuntimeException exception) {
            if (repository.markFailed(job)) repository.recordAudit(job.tenantId(), job.id(), null, "FAILED");
        }
    }

    private String csv(OperationalReport report) {
        return "metric,value\n"
                + "period_start," + report.periodStart() + "\n"
                + "period_end," + report.periodEnd() + "\n"
                + "residents_in_residence," + report.residentsInResidence() + "\n"
                + "enabled_beds," + report.enabledBeds() + "\n"
                + "occupied_beds," + report.occupiedBeds() + "\n"
                + "occupancy_rate_percent," + report.occupancyRate() + "\n"
                + "admission_applications," + report.admissionApplications() + "\n"
                + "discharges," + report.discharges() + "\n"
                + "scheduled_care_tasks," + report.scheduledCareTasks() + "\n"
                + "completed_care_tasks," + report.completedCareTasks() + "\n"
                + "exception_care_tasks," + report.exceptionCareTasks() + "\n"
                + "task_completion_rate_percent," + report.taskCompletionRate() + "\n"
                + "service_records," + report.serviceRecords() + "\n"
                + "open_follow_ups," + report.openFollowUps() + "\n";
    }
}
