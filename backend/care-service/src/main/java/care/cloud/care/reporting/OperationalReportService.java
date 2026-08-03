package care.cloud.care.reporting;

import care.cloud.care.security.TenantContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class OperationalReportService {
    private final OperationalReportRepository repository;

    public OperationalReportService(OperationalReportRepository repository) {
        this.repository = repository;
    }

    public OperationalReport report(LocalDate periodStart, LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);
        return repository.aggregate(TenantContext.requireCurrent().tenantId(), periodStart, periodEnd);
    }

    public void validatePeriod(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Report period is invalid");
        }
        if (ChronoUnit.DAYS.between(periodStart, periodEnd) > 366) {
            throw new IllegalArgumentException("Report period cannot exceed 367 days");
        }
    }
}
