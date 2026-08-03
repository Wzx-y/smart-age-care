package care.cloud.care.care;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCarePlanRepository implements CarePlanRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<CarePlan> rowMapper = (resultSet, rowNum) -> new CarePlan(
            resultSet.getLong("id"),
            resultSet.getLong("tenant_id"),
            resultSet.getLong("resident_id"),
            resultSet.getObject("assessment_id", Long.class),
            resultSet.getString("plan_name"),
            resultSet.getString("frequency_text"),
            resultSet.getLong("owner_id"),
            CarePlanStatus.valueOf(resultSet.getString("status")),
            resultSet.getLong("version"),
            resultSet.getObject("start_date", java.time.LocalDate.class),
            resultSet.getObject("end_date", java.time.LocalDate.class),
            offsetDateTime(resultSet.getTimestamp("published_at")),
            offsetDateTime(resultSet.getTimestamp("created_at"))
    );

    public JdbcCarePlanRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CarePlan create(CarePlan plan) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into care_plan (
                    tenant_id, resident_id, assessment_id, plan_name, frequency_text, owner_id, status, version,
                    start_date, end_date, created_by, created_at, updated_at
                ) values (
                    :tenantId, :residentId, :assessmentId, :planName, :frequencyText, :ownerId, :status, :version,
                    :startDate, :endDate, :createdBy, :createdAt, :createdAt
                )
                """, new MapSqlParameterSource()
                .addValue("tenantId", plan.tenantId())
                .addValue("residentId", plan.residentId())
                .addValue("assessmentId", plan.assessmentId())
                .addValue("planName", plan.planName())
                .addValue("frequencyText", plan.frequencyText())
                .addValue("ownerId", plan.ownerId())
                .addValue("status", plan.status().name())
                .addValue("version", plan.version())
                .addValue("startDate", plan.startDate())
                .addValue("endDate", plan.endDate())
                .addValue("createdBy", plan.ownerId())
                .addValue("createdAt", Timestamp.from(plan.createdAt().toInstant())), keyHolder, new String[]{"id"});

        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("创建护理计划后未返回主键");
        }
        return findByIdAndTenantId(generatedId.longValue(), plan.tenantId())
                .orElseThrow(() -> new IllegalStateException("创建护理计划后无法读取记录"));
    }

    @Override
    public Optional<CarePlan> findByIdAndTenantId(Long id, Long tenantId) {
        return jdbcTemplate.query("""
                select id, tenant_id, resident_id, assessment_id, plan_name, frequency_text, owner_id, status, version,
                       start_date, end_date, published_at, created_at
                from care_plan
                where id = :id and tenant_id = :tenantId
                """, new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId), rowMapper)
                .stream().findFirst();
    }

    @Override
    public List<CarePlan> findByTenantId(Long tenantId) {
        return jdbcTemplate.query("""
                select id, tenant_id, resident_id, assessment_id, plan_name, frequency_text, owner_id, status, version,
                       start_date, end_date, published_at, created_at
                from care_plan
                where tenant_id = :tenantId
                order by resident_id, version desc, id desc
                """, new MapSqlParameterSource().addValue("tenantId", tenantId), rowMapper);
    }

    @Override
    public void lockResident(Long tenantId, Long residentId) {
        List<Long> residentIds = jdbcTemplate.query("""
                select id from care_resident
                where id = :residentId and tenant_id = :tenantId and archived_at is null
                for update
                """, new MapSqlParameterSource().addValue("residentId", residentId).addValue("tenantId", tenantId),
                (resultSet, rowNum) -> resultSet.getLong("id"));
        if (residentIds.isEmpty()) {
            throw new CareNotFoundException("长者", residentId);
        }
    }

    @Override
    public boolean assessmentExists(Long tenantId, Long residentId, Long assessmentId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from care_resident_assessment
                where id = :assessmentId and tenant_id = :tenantId and resident_id = :residentId
                """, new MapSqlParameterSource().addValue("assessmentId", assessmentId)
                .addValue("tenantId", tenantId).addValue("residentId", residentId), Integer.class);
        return count != null && count == 1;
    }

    @Override
    public long nextVersion(Long tenantId, Long residentId) {
        Long version = jdbcTemplate.queryForObject("""
                select coalesce(max(version), 0) + 1
                from care_plan
                where tenant_id = :tenantId and resident_id = :residentId
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("residentId", residentId), Long.class);
        return version == null ? 1L : version;
    }

    @Override
    public boolean publish(CarePlan plan) {
        return jdbcTemplate.update("""
                update care_plan
                set status = 'PUBLISHED', published_at = current_timestamp(3), updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and status = 'DRAFT' and version = :version
                """, new MapSqlParameterSource().addValue("id", plan.id()).addValue("tenantId", plan.tenantId())
                .addValue("version", plan.version())) == 1;
    }

    @Override
    public boolean updateDraft(CarePlan plan) {
        return jdbcTemplate.update("""
                update care_plan set assessment_id = :assessmentId, plan_name = :planName, frequency_text = :frequencyText,
                    start_date = :startDate, end_date = :endDate, version = version + 1, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and status = 'DRAFT' and version = :version
                """, new MapSqlParameterSource().addValue("id", plan.id()).addValue("tenantId", plan.tenantId())
                .addValue("planName", plan.planName()).addValue("frequencyText", plan.frequencyText())
                .addValue("assessmentId", plan.assessmentId())
                .addValue("startDate", plan.startDate()).addValue("endDate", plan.endDate()).addValue("version", plan.version())) == 1;
    }

    @Override
    public boolean cancel(CarePlan plan) {
        return jdbcTemplate.update("""
                update care_plan set status = 'CANCELLED', version = version + 1, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and status in ('DRAFT', 'PUBLISHED') and version = :version
                """, new MapSqlParameterSource().addValue("id", plan.id()).addValue("tenantId", plan.tenantId())
                .addValue("version", plan.version())) == 1;
    }

    @Override
    public void supersedePublished(Long tenantId, Long residentId, Long exceptPlanId) {
        jdbcTemplate.update("""
                update care_plan
                set status = 'SUPERSEDED', updated_at = current_timestamp(3)
                where tenant_id = :tenantId and resident_id = :residentId and status = 'PUBLISHED' and id <> :exceptPlanId
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("residentId", residentId)
                .addValue("exceptPlanId", exceptPlanId));
    }

    private static OffsetDateTime offsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
