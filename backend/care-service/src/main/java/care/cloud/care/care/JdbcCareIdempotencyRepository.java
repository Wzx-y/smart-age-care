package care.cloud.care.care;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareIdempotencyRepository implements CareIdempotencyRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<CareIdempotencyRecord> rowMapper = (resultSet, rowNum) -> {
        Long resourceId = resultSet.getObject("result_resource_id", Long.class);
        CareWriteResult result = resourceId == null ? null : new CareWriteResult(
                resultSet.getString("result_resource_type"), resourceId, resultSet.getString("result_status"),
                resultSet.getLong("result_version"), resultSet.getString("result_detail")
        );
        return new CareIdempotencyRecord(
                resultSet.getLong("id"), resultSet.getLong("tenant_id"), resultSet.getLong("actor_id"),
                resultSet.getString("operation"), resultSet.getString("idempotency_key"),
                resultSet.getString("request_fingerprint"), CareIdempotencyStatus.valueOf(resultSet.getString("status")),
                result, resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
                resultSet.getTimestamp("completed_at") == null ? null : resultSet.getTimestamp("completed_at").toInstant().atOffset(ZoneOffset.UTC)
        );
    };

    public JdbcCareIdempotencyRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CareIdempotencyRecord> find(Long tenantId, Long actorId, String operation, String idempotencyKey) {
        return jdbcTemplate.query(selectSql(""), parameters(tenantId, actorId, operation, idempotencyKey), rowMapper).stream().findFirst();
    }

    @Override
    public Optional<CareIdempotencyRecord> findForUpdate(Long tenantId, Long actorId, String operation, String idempotencyKey) {
        return jdbcTemplate.query(selectSql(" for update"), parameters(tenantId, actorId, operation, idempotencyKey), rowMapper).stream().findFirst();
    }

    @Override
    public boolean begin(Long tenantId, Long actorId, String operation, String idempotencyKey, String requestFingerprint) {
        try {
            return jdbcTemplate.update("""
                    insert into care_write_idempotency (
                        tenant_id, actor_id, operation, idempotency_key, request_fingerprint, status, created_at
                    ) values (
                        :tenantId, :actorId, :operation, :idempotencyKey, :requestFingerprint, 'IN_PROGRESS', current_timestamp(3)
                    )
                    """, parameters(tenantId, actorId, operation, idempotencyKey)
                    .addValue("requestFingerprint", requestFingerprint)) == 1;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public void complete(Long tenantId, Long actorId, String operation, String idempotencyKey, CareWriteResult result) {
        int updated = jdbcTemplate.update("""
                update care_write_idempotency
                set status = 'COMPLETED', result_resource_type = :resourceType, result_resource_id = :resourceId,
                    result_status = :resultStatus, result_version = :resultVersion, result_detail = :resultDetail,
                    completed_at = current_timestamp(3)
                where tenant_id = :tenantId and actor_id = :actorId and operation = :operation
                  and idempotency_key = :idempotencyKey and status = 'IN_PROGRESS'
                """, parameters(tenantId, actorId, operation, idempotencyKey)
                .addValue("resourceType", result.resourceType()).addValue("resourceId", result.resourceId())
                .addValue("resultStatus", result.status()).addValue("resultVersion", result.version())
                .addValue("resultDetail", result.detail()));
        if (updated != 1) {
            throw new IllegalStateException("护理请求幂等结果写入失败");
        }
    }

    private MapSqlParameterSource parameters(Long tenantId, Long actorId, String operation, String idempotencyKey) {
        return new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("actorId", actorId)
                .addValue("operation", operation).addValue("idempotencyKey", idempotencyKey);
    }

    private String selectSql(String lockClause) {
        return """
                select id, tenant_id, actor_id, operation, idempotency_key, request_fingerprint, status,
                       result_resource_type, result_resource_id, result_status, result_version, result_detail,
                       created_at, completed_at
                from care_write_idempotency
                where tenant_id = :tenantId and actor_id = :actorId and operation = :operation
                  and idempotency_key = :idempotencyKey%s
                """.formatted(lockClause);
    }
}
