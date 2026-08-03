package care.cloud.care.export;

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
public class JdbcExportRepository implements ExportRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<ExportJob> rowMapper = (row, index) -> new ExportJob(
            row.getLong("id"), row.getLong("tenant_id"), ExportType.valueOf(row.getString("export_type")),
            row.getDate("period_start").toLocalDate(), row.getDate("period_end").toLocalDate(), ExportStatus.valueOf(row.getString("status")),
            row.getString("file_name"), row.getString("storage_key"), row.getLong("created_by"), offset(row.getTimestamp("created_at")),
            offset(row.getTimestamp("completed_at")), offset(row.getTimestamp("expires_at")), row.getLong("version"));

    public JdbcExportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ExportJob create(ExportJob job) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into care_export_job (tenant_id, export_type, period_start, period_end, status, created_by, created_at, expires_at, version)
                values (:tenantId, :exportType, :periodStart, :periodEnd, 'QUEUED', :createdBy, :createdAt, :expiresAt, 0)
                """, params(job), keys, new String[] { "id" });
        return new ExportJob(keys.getKey().longValue(), job.tenantId(), job.exportType(), job.periodStart(), job.periodEnd(),
                ExportStatus.QUEUED, null, null, job.createdBy(), job.createdAt(), null, job.expiresAt(), 0L);
    }

    @Override
    public List<ExportJob> findByTenantId(Long tenantId) {
        return jdbcTemplate.query("select * from care_export_job where tenant_id = :tenantId order by created_at desc, id desc", params(tenantId), rowMapper);
    }

    @Override
    public Optional<ExportJob> findByIdAndTenantId(Long id, Long tenantId) {
        return jdbcTemplate.query("select * from care_export_job where id = :id and tenant_id = :tenantId",
                params(tenantId).addValue("id", id), rowMapper).stream().findFirst();
    }

    @Override
    public Optional<ExportJob> claimNextQueued() {
        List<ExportJob> jobs = jdbcTemplate.query("select * from care_export_job where status = 'QUEUED' order by created_at, id limit 1", new MapSqlParameterSource(), rowMapper);
        if (jobs.isEmpty()) return Optional.empty();
        ExportJob queued = jobs.getFirst();
        int updated = jdbcTemplate.update("""
                update care_export_job set status = 'GENERATING', version = version + 1
                where id = :id and tenant_id = :tenantId and status = 'QUEUED' and version = :version
                """, params(queued.tenantId()).addValue("id", queued.id()).addValue("version", queued.version()));
        if (updated != 1) return Optional.empty();
        return Optional.of(new ExportJob(queued.id(), queued.tenantId(), queued.exportType(), queued.periodStart(), queued.periodEnd(),
                ExportStatus.GENERATING, null, null, queued.createdBy(), queued.createdAt(), null, queued.expiresAt(), queued.version() + 1));
    }

    @Override
    public boolean markReady(ExportJob job, String fileName, String storageKey, OffsetDateTime completedAt) {
        return jdbcTemplate.update("""
                update care_export_job set status = 'READY', file_name = :fileName, storage_key = :storageKey,
                    completed_at = :completedAt, failure_message = null, version = version + 1
                where id = :id and tenant_id = :tenantId and status = 'GENERATING' and version = :version
                """, params(job.tenantId()).addValue("id", job.id()).addValue("version", job.version())
                .addValue("fileName", fileName).addValue("storageKey", storageKey).addValue("completedAt", completedAt)) == 1;
    }

    @Override
    public boolean markFailed(ExportJob job) {
        return jdbcTemplate.update("""
                update care_export_job set status = 'FAILED', failure_message = 'Generation failed', completed_at = current_timestamp(3), version = version + 1
                where id = :id and tenant_id = :tenantId and status = 'GENERATING' and version = :version
                """, params(job.tenantId()).addValue("id", job.id()).addValue("version", job.version())) == 1;
    }

    @Override
    public List<ExportJob> findReadyExpiredBefore(OffsetDateTime now) {
        return jdbcTemplate.query("select * from care_export_job where status = 'READY' and expires_at <= :now order by expires_at, id",
                new MapSqlParameterSource().addValue("now", now), rowMapper);
    }

    @Override
    public boolean markExpired(ExportJob job) {
        return jdbcTemplate.update("""
                update care_export_job set status = 'EXPIRED', version = version + 1
                where id = :id and tenant_id = :tenantId and status = 'READY' and expires_at <= current_timestamp(3) and version = :version
                """, params(job.tenantId()).addValue("id", job.id()).addValue("version", job.version())) == 1;
    }

    @Override
    public void recordAudit(Long tenantId, Long exportJobId, Long actorId, String action) {
        jdbcTemplate.update("""
                insert into care_export_audit (tenant_id, export_job_id, actor_id, action, created_at)
                values (:tenantId, :exportJobId, :actorId, :action, current_timestamp(3))
                """, params(tenantId).addValue("exportJobId", exportJobId).addValue("actorId", actorId).addValue("action", action));
    }

    private MapSqlParameterSource params(Long tenantId) { return new MapSqlParameterSource().addValue("tenantId", tenantId); }
    private MapSqlParameterSource params(ExportJob job) { return params(job.tenantId()).addValue("exportType", job.exportType().name())
            .addValue("periodStart", job.periodStart()).addValue("periodEnd", job.periodEnd()).addValue("createdBy", job.createdBy())
            .addValue("createdAt", job.createdAt()).addValue("expiresAt", job.expiresAt()); }
    private OffsetDateTime offset(java.sql.Timestamp timestamp) { return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC); }
}
