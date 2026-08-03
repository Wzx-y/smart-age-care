package care.cloud.care.care;

import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareTaskGenerationRunRepository implements CareTaskGenerationRunRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCareTaskGenerationRunRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CareTaskGenerationRun create(CareTaskGenerationRun run) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into care_task_generation_run (tenant_id, service_date, generated_task_count, created_by, created_at)
                values (:tenantId, :serviceDate, :generatedTaskCount, :createdBy, current_timestamp(3))
                """, new MapSqlParameterSource().addValue("tenantId", run.tenantId()).addValue("serviceDate", run.serviceDate())
                .addValue("generatedTaskCount", run.generatedTaskCount()).addValue("createdBy", run.createdBy()), keyHolder, new String[]{"id"});
        Number id = keyHolder.getKey();
        if (id == null) throw new IllegalStateException("创建护理任务生成批次后未返回主键");
        return findByIdAndTenantId(id.longValue(), run.tenantId()).orElseThrow();
    }

    @Override
    public Optional<CareTaskGenerationRun> findByIdAndTenantId(Long id, Long tenantId) {
        return jdbcTemplate.query("""
                select id, tenant_id, service_date, generated_task_count, created_by, created_at
                from care_task_generation_run where id = :id and tenant_id = :tenantId
                """, new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId), (resultSet, rowNum) ->
                new CareTaskGenerationRun(resultSet.getLong("id"), resultSet.getLong("tenant_id"),
                        resultSet.getObject("service_date", java.time.LocalDate.class), resultSet.getInt("generated_task_count"),
                        resultSet.getLong("created_by"), resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC))).stream().findFirst();
    }
}
