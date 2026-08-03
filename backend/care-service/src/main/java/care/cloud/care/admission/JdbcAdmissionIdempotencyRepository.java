package care.cloud.care.admission;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAdmissionIdempotencyRepository implements AdmissionIdempotencyRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<AdmissionIdempotencyRecord> rowMapper = (resultSet, rowNum) -> {
        Long resultAdmissionId = resultSet.getObject("result_admission_id", Long.class);
        Admission result = resultAdmissionId == null ? null : new Admission(
                resultAdmissionId,
                resultSet.getLong("tenant_id"),
                resultSet.getObject("result_resident_id", Long.class),
                resultSet.getObject("result_bed_id", Long.class),
                null,
                AdmissionStatus.valueOf(resultSet.getString("result_admission_status")),
                resultSet.getLong("result_admission_version"),
                resultSet.getTimestamp("result_applied_at").toInstant().atOffset(ZoneOffset.UTC)
        );
        return new AdmissionIdempotencyRecord(
                resultSet.getLong("id"),
                resultSet.getLong("tenant_id"),
                resultSet.getLong("actor_id"),
                resultSet.getString("operation"),
                resultSet.getString("idempotency_key"),
                resultSet.getString("request_fingerprint"),
                AdmissionIdempotencyStatus.valueOf(resultSet.getString("status")),
                result,
                resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
                resultSet.getTimestamp("completed_at") == null ? null : resultSet.getTimestamp("completed_at").toInstant().atOffset(ZoneOffset.UTC)
        );
    };

    public JdbcAdmissionIdempotencyRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AdmissionIdempotencyRecord> find(Long tenantId, Long actorId, String operation, String idempotencyKey) {
        List<AdmissionIdempotencyRecord> records = jdbcTemplate.query(selectSql(""), parameters(tenantId, actorId, operation, idempotencyKey), rowMapper);
        return records.stream().findFirst();
    }

    @Override
    public Optional<AdmissionIdempotencyRecord> findForUpdate(Long tenantId, Long actorId, String operation, String idempotencyKey) {
        List<AdmissionIdempotencyRecord> records = jdbcTemplate.query(selectSql(" for update"), parameters(tenantId, actorId, operation, idempotencyKey), rowMapper);
        return records.stream().findFirst();
    }

    @Override
    public boolean begin(Long tenantId, Long actorId, String operation, String idempotencyKey, String requestFingerprint) {
        try {
            int inserted = jdbcTemplate.update("""
                    insert into care_admission_idempotency (
                        tenant_id, actor_id, operation, idempotency_key, request_fingerprint, status, created_at
                    ) values (
                        :tenantId, :actorId, :operation, :idempotencyKey, :requestFingerprint, 'IN_PROGRESS', current_timestamp(3)
                    )
                    """, parameters(tenantId, actorId, operation, idempotencyKey)
                    .addValue("requestFingerprint", requestFingerprint));
            return inserted == 1;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public void complete(Long tenantId, Long actorId, String operation, String idempotencyKey, Admission result) {
        int updated = jdbcTemplate.update("""
                update care_admission_idempotency
                set status = 'COMPLETED',
                    result_admission_id = :admissionId,
                    result_resident_id = :residentId,
                    result_bed_id = :bedId,
                    result_admission_status = :admissionStatus,
                    result_admission_version = :admissionVersion,
                    result_applied_at = :appliedAt,
                    completed_at = current_timestamp(3)
                where tenant_id = :tenantId
                  and actor_id = :actorId
                  and operation = :operation
                  and idempotency_key = :idempotencyKey
                  and status = 'IN_PROGRESS'
                """, parameters(tenantId, actorId, operation, idempotencyKey)
                .addValue("admissionId", result.id())
                .addValue("residentId", result.residentId())
                .addValue("bedId", result.bedId())
                .addValue("admissionStatus", result.status().name())
                .addValue("admissionVersion", result.version())
                .addValue("appliedAt", Timestamp.from(result.appliedAt().toInstant())));
        if (updated != 1) {
            throw new IllegalStateException("入住请求幂等结果写入失败");
        }
    }

    private MapSqlParameterSource parameters(Long tenantId, Long actorId, String operation, String idempotencyKey) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("actorId", actorId)
                .addValue("operation", operation)
                .addValue("idempotencyKey", idempotencyKey);
    }

    private String selectSql(String lockClause) {
        return """
                select id, tenant_id, actor_id, operation, idempotency_key, request_fingerprint, status,
                       result_admission_id, result_resident_id, result_bed_id, result_admission_status,
                       result_admission_version, result_applied_at, created_at, completed_at
                from care_admission_idempotency
                where tenant_id = :tenantId
                  and actor_id = :actorId
                  and operation = :operation
                  and idempotency_key = :idempotencyKey%s
                """.formatted(lockClause);
    }
}
