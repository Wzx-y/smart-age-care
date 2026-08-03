package care.cloud.care.reporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOperationalReportRepository implements OperationalReportRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcOperationalReportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OperationalReport aggregate(Long tenantId, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate exclusiveEnd = periodEnd.plusDays(1);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("from", java.sql.Timestamp.from(periodStart.atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()))
                .addValue("to", java.sql.Timestamp.from(exclusiveEnd.atStartOfDay().atOffset(ZoneOffset.UTC).toInstant()));
        return jdbcTemplate.queryForObject("""
                select
                  (select count(1) from care_resident where tenant_id = :tenantId and archived_at is null and status in ('IN_RESIDENCE', 'AWAY')) as residents_in_residence,
                  (select count(1) from care_bed bed join care_room room on room.id = bed.room_id and room.tenant_id = bed.tenant_id
                   where bed.tenant_id = :tenantId and bed.enabled = '0' and room.enabled = '0') as enabled_beds,
                  (select count(1) from care_bed bed join care_room room on room.id = bed.room_id and room.tenant_id = bed.tenant_id
                   where bed.tenant_id = :tenantId and bed.enabled = '0' and room.enabled = '0' and bed.occupancy_status = 'OCCUPIED') as occupied_beds,
                  (select count(1) from care_admission where tenant_id = :tenantId and applied_at >= :from and applied_at < :to) as admission_applications,
                  (select count(1) from care_admission where tenant_id = :tenantId and discharged_at >= :from and discharged_at < :to) as discharges,
                  (select count(1) from care_task where tenant_id = :tenantId and scheduled_at >= :from and scheduled_at < :to and status <> 'CANCELLED') as scheduled_tasks,
                  (select count(1) from care_task where tenant_id = :tenantId and scheduled_at >= :from and scheduled_at < :to and status = 'COMPLETED') as completed_tasks,
                  (select count(1) from care_task where tenant_id = :tenantId and scheduled_at >= :from and scheduled_at < :to and status = 'EXCEPTION') as exception_tasks,
                  (select count(1) from care_service_record where tenant_id = :tenantId and completed_at >= :from and completed_at < :to) as service_records,
                  (select count(1) from care_task_follow_up where tenant_id = :tenantId and status = 'OPEN') as open_follow_ups
                """, parameters, (row, index) -> {
            long enabledBeds = row.getLong("enabled_beds");
            long occupiedBeds = row.getLong("occupied_beds");
            long scheduledTasks = row.getLong("scheduled_tasks");
            long completedTasks = row.getLong("completed_tasks");
            return new OperationalReport(periodStart, periodEnd,
                    row.getLong("residents_in_residence"), enabledBeds, occupiedBeds, ratio(occupiedBeds, enabledBeds),
                    row.getLong("admission_applications"), row.getLong("discharges"), scheduledTasks, completedTasks,
                    row.getLong("exception_tasks"), ratio(completedTasks, scheduledTasks), row.getLong("service_records"),
                    row.getLong("open_follow_ups"));
        });
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
