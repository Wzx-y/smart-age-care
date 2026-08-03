package care.cloud.care.reporting;

import java.time.LocalDate;

public interface OperationalReportRepository {
    OperationalReport aggregate(Long tenantId, LocalDate periodStart, LocalDate periodEnd);
}
