package care.cloud.care.care;

import care.cloud.care.security.TenantContext;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CareIdempotencyService {
    private final CareIdempotencyRepository repository;

    public CareIdempotencyService(CareIdempotencyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public <T> T execute(
            String operation,
            String idempotencyKey,
            String requestFingerprint,
            Supplier<T> action,
            Function<T, CareWriteResult> resultMapper,
            Function<CareWriteResult, T> replayer
    ) {
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > 128) {
            throw new CareInvalidIdempotencyKeyException();
        }
        var principal = TenantContext.requireCurrent();
        var existing = repository.find(principal.tenantId(), principal.userId(), operation, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), requestFingerprint, replayer);
        }
        if (!repository.begin(principal.tenantId(), principal.userId(), operation, idempotencyKey, requestFingerprint)) {
            return replay(repository.findForUpdate(principal.tenantId(), principal.userId(), operation, idempotencyKey)
                    .orElseThrow(CareIdempotencyRequestInProgressException::new), requestFingerprint, replayer);
        }

        T result = action.get();
        repository.complete(principal.tenantId(), principal.userId(), operation, idempotencyKey, resultMapper.apply(result));
        return result;
    }

    private <T> T replay(CareIdempotencyRecord existing, String requestFingerprint, Function<CareWriteResult, T> replayer) {
        if (!existing.requestFingerprint().equals(requestFingerprint)) {
            throw new CareIdempotencyKeyConflictException();
        }
        if (existing.status() != CareIdempotencyStatus.COMPLETED || existing.result() == null) {
            throw new CareIdempotencyRequestInProgressException();
        }
        return replayer.apply(existing.result());
    }
}
