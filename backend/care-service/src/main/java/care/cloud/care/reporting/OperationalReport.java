package care.cloud.care.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OperationalReport(
        LocalDate periodStart,
        LocalDate periodEnd,
        long residentsInResidence,
        long enabledBeds,
        long occupiedBeds,
        BigDecimal occupancyRate,
        long admissionApplications,
        long discharges,
        long scheduledCareTasks,
        long completedCareTasks,
        long exceptionCareTasks,
        BigDecimal taskCompletionRate,
        long serviceRecords,
        long openFollowUps
) {
}
