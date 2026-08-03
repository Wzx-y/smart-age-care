package care.cloud.care.admission;

import care.cloud.care.security.TenantContext;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdmissionIdempotencyService {
    private final AdmissionIdempotencyRepository repository;

    public AdmissionIdempotencyService(AdmissionIdempotencyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Admission execute(String operation, String idempotencyKey, String requestFingerprint, Supplier<Admission> action) {
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > 128) {
            throw new InvalidIdempotencyKeyException();
        }
        var principal = TenantContext.requireCurrent();
        var existing = repository.find(principal.tenantId(), principal.userId(), operation, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestFingerprint);
        }
        if (!repository.begin(principal.tenantId(), principal.userId(), operation, idempotencyKey, requestFingerprint)) {
            return replay(repository.findForUpdate(principal.tenantId(), principal.userId(), operation, idempotencyKey)
                    .orElseThrow(IdempotencyRequestInProgressException::new), requestFingerprint);
        }

        Admission result = action.get();
        repository.complete(principal.tenantId(), principal.userId(), operation, idempotencyKey, result);
        return result;
    }

    private Admission replay(AdmissionIdempotencyRecord existing, String requestFingerprint) {
        if (!existing.requestFingerprint().equals(requestFingerprint)) {
            throw new IdempotencyKeyConflictException();
        }
        if (existing.status() != AdmissionIdempotencyStatus.COMPLETED || existing.result() == null) {
            throw new IdempotencyRequestInProgressException();
        }
        return existing.result();
    }
}
