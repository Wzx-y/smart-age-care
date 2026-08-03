package care.cloud.care.resident;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResidentAuditWriter {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ResidentAuditWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void write(Long tenantId, Long actorId, String action, String targetType, Long targetId, Long residentId) {
        jdbcTemplate.update("""
                insert into care_audit_log (tenant_id, actor_id, action, target_type, target_id, related_resident_id, created_at)
                values (:tenantId, :actorId, :action, :targetType, :targetId, :residentId, current_timestamp(3))
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("actorId", actorId)
                .addValue("action", action).addValue("targetType", targetType).addValue("targetId", targetId)
                .addValue("residentId", residentId));
    }
}
