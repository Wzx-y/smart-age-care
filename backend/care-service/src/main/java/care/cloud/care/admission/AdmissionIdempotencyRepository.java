package care.cloud.care.admission;

import java.util.Optional;

public interface AdmissionIdempotencyRepository {
    Optional<AdmissionIdempotencyRecord> find(Long tenantId, Long actorId, String operation, String idempotencyKey);

    Optional<AdmissionIdempotencyRecord> findForUpdate(Long tenantId, Long actorId, String operation, String idempotencyKey);

    boolean begin(Long tenantId, Long actorId, String operation, String idempotencyKey, String requestFingerprint);

    void complete(Long tenantId, Long actorId, String operation, String idempotencyKey, Admission result);
}
