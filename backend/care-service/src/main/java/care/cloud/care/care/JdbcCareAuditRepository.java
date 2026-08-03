package care.cloud.care.care;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareAuditRepository implements CareAuditRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCareAuditRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void recordTaskAction(Long tenantId, Long actorId, String action, CareTask task) {
        jdbcTemplate.update("""
                insert into care_audit_log (
                    tenant_id, actor_id, action, target_type, target_id, related_resident_id, created_at
                ) values (
                    :tenantId, :actorId, :action, 'CARE_TASK', :taskId, :residentId, current_timestamp(3)
                )
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("actorId", actorId)
                .addValue("action", action).addValue("taskId", task.id()).addValue("residentId", task.residentId()));
    }
}
