package care.cloud.care.care;

import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCarePlanTaskTemplateRepository implements CarePlanTaskTemplateRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<CarePlanTaskTemplate> rowMapper = (resultSet, rowNum) -> new CarePlanTaskTemplate(
            resultSet.getLong("id"), resultSet.getLong("tenant_id"), resultSet.getLong("plan_id"), resultSet.getLong("resident_id"),
            resultSet.getString("task_name"), resultSet.getTime("scheduled_time").toLocalTime(), resultSet.getLong("assignee_id"),
            resultSet.getBoolean("active"), resultSet.getLong("version")
    );

    public JdbcCarePlanTaskTemplateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CarePlanTaskTemplate create(CarePlanTaskTemplate template, Long actorId) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into care_plan_task_template (
                    tenant_id, plan_id, task_name, scheduled_time, assignee_id, active, version, created_by, created_at, updated_at
                ) values (
                    :tenantId, :planId, :taskName, :scheduledTime, :assigneeId, true, 0, :actorId, current_timestamp(3), current_timestamp(3)
                )
                """, new MapSqlParameterSource().addValue("tenantId", template.tenantId()).addValue("planId", template.planId())
                .addValue("taskName", template.taskName()).addValue("scheduledTime", Time.valueOf(template.scheduledTime()))
                .addValue("assigneeId", template.assigneeId()).addValue("actorId", actorId), keyHolder, new String[]{"id"});
        Number id = keyHolder.getKey();
        if (id == null) throw new IllegalStateException("创建护理任务模板后未返回主键");
        return new CarePlanTaskTemplate(id.longValue(), template.tenantId(), template.planId(), template.residentId(), template.taskName(),
                template.scheduledTime(), template.assigneeId(), true, 0L);
    }

    @Override
    public Optional<CarePlanTaskTemplate> findByIdAndTenantId(Long id, Long tenantId) {
        return jdbcTemplate.query("""
                select template.id, template.tenant_id, template.plan_id, plan.resident_id, template.task_name,
                       template.scheduled_time, template.assignee_id, template.active, template.version
                from care_plan_task_template template
                join care_plan plan on plan.id = template.plan_id and plan.tenant_id = template.tenant_id
                where template.id = :id and template.tenant_id = :tenantId
                """, new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId), rowMapper).stream().findFirst();
    }

    @Override
    public List<CarePlanTaskTemplate> findByPlanIdAndTenantId(Long planId, Long tenantId) {
        return jdbcTemplate.query("""
                select template.id, template.tenant_id, template.plan_id, plan.resident_id, template.task_name,
                       template.scheduled_time, template.assignee_id, template.active, template.version
                from care_plan_task_template template
                join care_plan plan on plan.id = template.plan_id and plan.tenant_id = template.tenant_id
                where template.plan_id = :planId and template.tenant_id = :tenantId
                order by template.scheduled_time, template.id
                """, new MapSqlParameterSource().addValue("planId", planId).addValue("tenantId", tenantId), rowMapper);
    }

    @Override
    public List<CarePlanTaskTemplate> findActiveForPublishedPlans(Long tenantId, LocalDate serviceDate) {
        return jdbcTemplate.query("""
                select template.id, template.tenant_id, template.plan_id, plan.resident_id, template.task_name,
                       template.scheduled_time, template.assignee_id, template.active, template.version
                from care_plan_task_template template
                join care_plan plan on plan.id = template.plan_id and plan.tenant_id = template.tenant_id
                where template.tenant_id = :tenantId and template.active = true and plan.status = 'PUBLISHED'
                  and plan.start_date <= :serviceDate and (plan.end_date is null or plan.end_date >= :serviceDate)
                order by template.plan_id, template.scheduled_time, template.id
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("serviceDate", serviceDate), rowMapper);
    }

    @Override
    public boolean update(CarePlanTaskTemplate template) {
        return jdbcTemplate.update("""
                update care_plan_task_template set task_name = :taskName, scheduled_time = :scheduledTime,
                    assignee_id = :assigneeId, version = version + 1, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and active = true and version = :version
                """, new MapSqlParameterSource().addValue("id", template.id()).addValue("tenantId", template.tenantId())
                .addValue("taskName", template.taskName()).addValue("scheduledTime", Time.valueOf(template.scheduledTime()))
                .addValue("assigneeId", template.assigneeId()).addValue("version", template.version())) == 1;
    }

    @Override
    public boolean deactivate(CarePlanTaskTemplate template) {
        return jdbcTemplate.update("""
                update care_plan_task_template set active = false, version = version + 1, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and active = true and version = :version
                """, new MapSqlParameterSource().addValue("id", template.id()).addValue("tenantId", template.tenantId())
                .addValue("version", template.version())) == 1;
    }
}
