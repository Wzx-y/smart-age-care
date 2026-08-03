package care.cloud.care.care;

import java.util.Optional;

public interface CareIdempotencyRepository {
    Optional<CareIdempotencyRecord> find(Long tenantId, Long actorId, String operation, String idempotencyKey);

    Optional<CareIdempotencyRecord> findForUpdate(Long tenantId, Long actorId, String operation, String idempotencyKey);

    boolean begin(Long tenantId, Long actorId, String operation, String idempotencyKey, String requestFingerprint);

    void complete(Long tenantId, Long actorId, String operation, String idempotencyKey, CareWriteResult result);
}
