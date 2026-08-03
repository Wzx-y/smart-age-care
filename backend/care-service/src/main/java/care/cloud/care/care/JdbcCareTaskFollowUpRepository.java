package care.cloud.care.care;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareTaskFollowUpRepository implements CareTaskFollowUpRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<CareTaskFollowUp> rowMapper = (resultSet, rowNum) -> new CareTaskFollowUp(
            resultSet.getLong("id"), resultSet.getLong("tenant_id"), resultSet.getLong("task_id"), resultSet.getLong("resident_id"),
            CareTaskFollowUpStatus.valueOf(resultSet.getString("status")), resultSet.getString("exception_reason"),
            resultSet.getString("resolution_note"), resultSet.getLong("created_by"),
            resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC), resultSet.getObject("closed_by", Long.class),
            resultSet.getTimestamp("closed_at") == null ? null : resultSet.getTimestamp("closed_at").toInstant().atOffset(ZoneOffset.UTC),
            resultSet.getLong("version")
    );

    public JdbcCareTaskFollowUpRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CareTaskFollowUp> findByIdAndTenantId(Long id, Long tenantId) {
        return jdbcTemplate.query(selectSql("where id = :id and tenant_id = :tenantId"),
                new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId), rowMapper).stream().findFirst();
    }

    @Override
    public List<CareTaskFollowUp> findByTenantId(Long tenantId, CareTaskFollowUpStatus status) {
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("tenantId", tenantId);
        String statusClause = "";
        if (status != null) {
            statusClause = " and status = :status";
            parameters.addValue("status", status.name());
        }
        return jdbcTemplate.query(selectSql("where tenant_id = :tenantId" + statusClause + " order by created_at desc, id desc"), parameters, rowMapper);
    }

    @Override
    public long countOpenByTaskIdsAndTenantId(List<Long> taskIds, Long tenantId) {
        if (taskIds.isEmpty()) return 0;
        Long count = jdbcTemplate.queryForObject("""
                select count(*) from care_task_follow_up
                where tenant_id = :tenantId and status = 'OPEN' and task_id in (:taskIds)
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("taskIds", taskIds), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public boolean resolve(CareTaskFollowUp followUp, Long actorId, String resolutionNote) {
        return jdbcTemplate.update("""
                update care_task_follow_up
                set status = 'RESOLVED', resolution_note = :resolutionNote, closed_by = :actorId,
                    closed_at = current_timestamp(3), version = version + 1
                where id = :id and tenant_id = :tenantId and status = 'OPEN' and version = :version
                """, new MapSqlParameterSource().addValue("id", followUp.id()).addValue("tenantId", followUp.tenantId())
                .addValue("version", followUp.version()).addValue("actorId", actorId).addValue("resolutionNote", resolutionNote)) == 1;
    }

    private String selectSql(String condition) {
        return """
                select id, tenant_id, task_id, resident_id, status, exception_reason, resolution_note,
                       created_by, created_at, closed_by, closed_at, version
                from care_task_follow_up %s
                """.formatted(condition);
    }
}
