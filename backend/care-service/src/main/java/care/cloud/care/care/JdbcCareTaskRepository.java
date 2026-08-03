package care.cloud.care.care;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareTaskRepository implements CareTaskRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<CareTask> rowMapper = (resultSet, rowNum) -> new CareTask(
            resultSet.getLong("id"),
            resultSet.getLong("tenant_id"),
            resultSet.getLong("plan_id"),
            resultSet.getLong("resident_id"),
            resultSet.getString("task_name"),
            resultSet.getTimestamp("scheduled_at").toInstant().atOffset(ZoneOffset.UTC),
            resultSet.getLong("assignee_id"),
            CareTaskStatus.valueOf(resultSet.getString("status")),
            resultSet.getString("exception_reason"),
            resultSet.getLong("version")
    );

    public JdbcCareTaskRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CareTask create(CareTask task) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into care_task (
                    tenant_id, plan_id, resident_id, task_name, scheduled_at, assignee_id, status,
                    version, created_at, updated_at
                ) values (
                    :tenantId, :planId, :residentId, :taskName, :scheduledAt, :assigneeId, :status,
                    :version, current_timestamp(3), current_timestamp(3)
                )
                """, new MapSqlParameterSource()
                .addValue("tenantId", task.tenantId()).addValue("planId", task.planId())
                .addValue("residentId", task.residentId()).addValue("taskName", task.taskName())
                .addValue("scheduledAt", Timestamp.from(task.scheduledAt().toInstant()))
                .addValue("assigneeId", task.assigneeId()).addValue("status", task.status().name())
                .addValue("version", task.version()), keyHolder, new String[]{"id"});
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("创建护理任务后未返回主键");
        }
        return findByIdAndTenantId(generatedId.longValue(), task.tenantId())
                .orElseThrow(() -> new IllegalStateException("创建护理任务后无法读取记录"));
    }

    @Override
    public boolean createFromTemplate(CarePlanTaskTemplate template, LocalDate serviceDate) {
        OffsetDateTime scheduledAt = OffsetDateTime.of(serviceDate, template.scheduledTime(), ZoneOffset.UTC);
        return jdbcTemplate.update("""
                insert ignore into care_task (
                    tenant_id, plan_id, resident_id, task_name, scheduled_at, assignee_id, status, version,
                    origin_template_id, service_date, created_at, updated_at
                ) values (
                    :tenantId, :planId, :residentId, :taskName, :scheduledAt, :assigneeId, 'PENDING', 0,
                    :templateId, :serviceDate, current_timestamp(3), current_timestamp(3)
                )
                """, new MapSqlParameterSource().addValue("tenantId", template.tenantId()).addValue("planId", template.planId())
                .addValue("residentId", template.residentId()).addValue("taskName", template.taskName())
                .addValue("scheduledAt", Timestamp.from(scheduledAt.toInstant())).addValue("assigneeId", template.assigneeId())
                .addValue("templateId", template.id()).addValue("serviceDate", serviceDate)) == 1;
    }

    @Override
    public Optional<CareTask> findByIdAndTenantId(Long id, Long tenantId) {
        return jdbcTemplate.query("""
                select id, tenant_id, plan_id, resident_id, task_name, scheduled_at, assignee_id, status,
                       exception_reason, version
                from care_task
                where id = :id and tenant_id = :tenantId
                """, new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId), rowMapper)
                .stream().findFirst();
    }

    @Override
    public List<CareTask> findByIdsAndTenantId(List<Long> ids, Long tenantId) {
        if (ids.isEmpty()) return List.of();
        return jdbcTemplate.query("""
                select id, tenant_id, plan_id, resident_id, task_name, scheduled_at, assignee_id, status,
                       exception_reason, version
                from care_task
                where tenant_id = :tenantId and id in (:ids)
                order by id asc
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("ids", ids), rowMapper);
    }

    @Override
    public List<CareTask> findByTenantId(Long tenantId) {
        return jdbcTemplate.query("""
                select id, tenant_id, plan_id, resident_id, task_name, scheduled_at, assignee_id, status,
                       exception_reason, version
                from care_task
                where tenant_id = :tenantId
                order by scheduled_at asc, id asc
                """, new MapSqlParameterSource().addValue("tenantId", tenantId), rowMapper);
    }

    @Override
    public List<CareTask> findUnfinishedScheduledBefore(OffsetDateTime scheduledBefore) {
        return jdbcTemplate.query("""
                select id, tenant_id, plan_id, resident_id, task_name, scheduled_at, assignee_id, status,
                       exception_reason, version from care_task
                where scheduled_at < :scheduledBefore and status in ('PENDING', 'IN_PROGRESS', 'PENDING_CONFIRMATION')
                order by scheduled_at asc, id asc limit 500
                """, new MapSqlParameterSource().addValue("scheduledBefore", Timestamp.from(scheduledBefore.toInstant())), rowMapper);
    }

    @Override
    public boolean start(CareTask task, Long actorId) {
        return jdbcTemplate.update("""
                update care_task
                set status = 'IN_PROGRESS', version = version + 1, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and version = :version and status = 'PENDING'
                """, taskParameters(task).addValue("actorId", actorId)) == 1;
    }

    @Override
    public boolean complete(CareTask task, Long actorId) {
        return jdbcTemplate.update("""
                update care_task
                set status = 'COMPLETED', completed_at = current_timestamp(3), completed_by = :actorId,
                    version = version + 1, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and version = :version
                  and status in ('PENDING', 'IN_PROGRESS', 'PENDING_CONFIRMATION')
                """, taskParameters(task).addValue("actorId", actorId)) == 1;
    }

    @Override
    public boolean markException(CareTask task, String reason) {
        return jdbcTemplate.update("""
                update care_task
                set status = 'EXCEPTION', exception_reason = :reason, version = version + 1,
                    updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and version = :version
                  and status in ('PENDING', 'IN_PROGRESS', 'PENDING_CONFIRMATION')
                """, taskParameters(task).addValue("reason", reason)) == 1;
    }

    @Override
    public void createExceptionFollowUp(Long tenantId, CareTask task, Long actorId, String reason) {
        jdbcTemplate.update("""
                insert into care_task_follow_up (
                    tenant_id, task_id, resident_id, status, exception_reason, created_by, created_at
                ) values (
                    :tenantId, :taskId, :residentId, 'OPEN', :reason, :actorId, current_timestamp(3)
                )
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("taskId", task.id())
                .addValue("residentId", task.residentId()).addValue("reason", reason).addValue("actorId", actorId));
    }

    @Override
    public void createServiceRecord(Long tenantId, Long taskId, Long actorId, String resultNote) {
        jdbcTemplate.update("""
                insert into care_service_record (
                    tenant_id, task_id, executor_id, result_note, completed_at, created_at
                ) values (
                    :tenantId, :taskId, :actorId, :resultNote, current_timestamp(3), current_timestamp(3)
                )
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("taskId", taskId)
                .addValue("actorId", actorId).addValue("resultNote", resultNote));
    }

    @Override
    public List<CareServiceRecord> findServiceRecords(Long tenantId, Long residentId, LocalDate serviceDate, Long executorId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("tenantId", tenantId)
                .addValue("residentId", residentId).addValue("executorId", executorId);
        String residentClause = residentId == null ? "" : " and task.resident_id = :residentId";
        String executorClause = executorId == null ? "" : " and record.executor_id = :executorId";
        String dateClause = "";
        if (serviceDate != null) {
            dateClause = " and record.completed_at >= :serviceDateStart and record.completed_at < :serviceDateEnd";
            parameters.addValue("serviceDateStart", Timestamp.from(serviceDate.atStartOfDay().toInstant(ZoneOffset.UTC)))
                    .addValue("serviceDateEnd", Timestamp.from(serviceDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
        }
        return jdbcTemplate.query(("""
                select record.id, record.task_id, task.resident_id, record.executor_id, record.result_note, record.completed_at
                from care_service_record record join care_task task on task.id = record.task_id and task.tenant_id = record.tenant_id
                where record.tenant_id = :tenantId%s%s%s order by record.completed_at desc, record.id desc
                """).formatted(residentClause, dateClause, executorClause), parameters, (resultSet, rowNum) -> new CareServiceRecord(
                resultSet.getLong("id"), resultSet.getLong("task_id"), resultSet.getLong("resident_id"), resultSet.getLong("executor_id"),
                resultSet.getString("result_note"), resultSet.getTimestamp("completed_at").toInstant().atOffset(ZoneOffset.UTC)));
    }

    private MapSqlParameterSource taskParameters(CareTask task) {
        return new MapSqlParameterSource().addValue("id", task.id()).addValue("tenantId", task.tenantId())
                .addValue("version", task.version());
    }
}
