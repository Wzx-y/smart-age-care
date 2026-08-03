package care.cloud.care.audit;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditEventRepository implements AuditEventRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAuditEventRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AuditEvent> findByTenantId(Long tenantId, String resourceType, String action, Long actorId, int limit) {
        String filters = " where tenant_id = :tenantId"
                + (resourceType == null ? "" : " and resource_type = :resourceType")
                + (action == null ? "" : " and action = :action")
                + (actorId == null ? "" : " and actor_id = :actorId");
        String query = ("""
                select audit_source, event_id, action, resource_type, resource_id, related_resident_id, actor_id, created_at
                from (
                    select 'CARE' as audit_source, id as event_id, tenant_id, action, target_type as resource_type,
                           target_id as resource_id, related_resident_id, actor_id, created_at from care_audit_log
                    union all
                    select 'EXPORT', id, tenant_id, action, 'EXPORT_JOB', export_job_id, null, actor_id, created_at from care_export_audit
                    union all
                    select 'MASTER_DATA', id, tenant_id, action, resource_type, resource_id, null, actor_id, created_at from care_master_data_audit
                ) audit_events%s order by created_at desc, event_id desc limit :limit
                """).formatted(filters);
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("limit", limit)
                .addValue("resourceType", resourceType).addValue("action", action).addValue("actorId", actorId);
        return jdbcTemplate.query(query, parameters, (row, index) -> new AuditEvent(row.getString("audit_source"), row.getLong("event_id"),
                row.getString("action"), row.getString("resource_type"), row.getLong("resource_id"),
                (Long) row.getObject("related_resident_id"), (Long) row.getObject("actor_id"),
                row.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC)));
    }

    @Override
    public void purgeBefore(OffsetDateTime cutoff) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("cutoff", cutoff);
        jdbcTemplate.update("delete from care_audit_log where created_at < :cutoff", parameters);
        jdbcTemplate.update("delete from care_export_audit where created_at < :cutoff", parameters);
        jdbcTemplate.update("delete from care_master_data_audit where created_at < :cutoff", parameters);
    }
}
