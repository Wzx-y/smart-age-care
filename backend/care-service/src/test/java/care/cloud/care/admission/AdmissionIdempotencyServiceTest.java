package care.cloud.care.admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class AdmissionIdempotencyServiceTest {
    private final InMemoryAdmissionIdempotencyRepository repository = new InMemoryAdmissionIdempotencyRepository();
    private final AdmissionIdempotencyService service = new AdmissionIdempotencyService(repository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void executesOnceAndReplaysCompletedResultForSameRequest() {
        TenantContext.set(new TenantPrincipal(801L, 100L));
        AtomicInteger calls = new AtomicInteger();

        Admission first = service.execute("ASSIGN_BED", "request-1", "fingerprint-1", () -> {
            calls.incrementAndGet();
            return admission(4L, AdmissionStatus.PENDING_CONFIRMATION);
        });
        Admission replay = service.execute("ASSIGN_BED", "request-1", "fingerprint-1", () -> {
            calls.incrementAndGet();
            return admission(99L, AdmissionStatus.ADMITTED);
        });

        assertEquals(1, calls.get());
        assertEquals(first, replay);
        assertEquals(AdmissionIdempotencyStatus.COMPLETED, repository.find(100L, 801L, "ASSIGN_BED", "request-1").orElseThrow().status());
    }

    @Test
    void rejectsSameKeyWhenPayloadFingerprintChanges() {
        TenantContext.set(new TenantPrincipal(801L, 100L));
        service.execute("ASSIGN_BED", "request-1", "fingerprint-1", () -> admission(4L, AdmissionStatus.PENDING_CONFIRMATION));

        assertThrows(IdempotencyKeyConflictException.class, () -> service.execute(
                "ASSIGN_BED", "request-1", "different-fingerprint", () -> admission(5L, AdmissionStatus.PENDING_CONFIRMATION)
        ));
    }

    @Test
    void scopesSameKeyByCurrentTenantAndActor() {
        AtomicInteger calls = new AtomicInteger();
        TenantContext.set(new TenantPrincipal(801L, 100L));
        service.execute("CONFIRM_ADMISSION", "request-1", "fingerprint-1", () -> {
            calls.incrementAndGet();
            return admission(5L, AdmissionStatus.ADMITTED);
        });

        TenantContext.set(new TenantPrincipal(802L, 101L));
        service.execute("CONFIRM_ADMISSION", "request-1", "fingerprint-1", () -> {
            calls.incrementAndGet();
            return admission(6L, AdmissionStatus.ADMITTED);
        });

        assertEquals(2, calls.get());
    }

    @Test
    void rejectsRequestThatIsAlreadyInProgress() {
        TenantContext.set(new TenantPrincipal(801L, 100L));
        repository.begin(100L, 801L, "ASSIGN_BED", "request-1", "fingerprint-1");

        assertThrows(IdempotencyRequestInProgressException.class, () -> service.execute(
                "ASSIGN_BED", "request-1", "fingerprint-1", () -> admission(4L, AdmissionStatus.PENDING_CONFIRMATION)
        ));
    }

    @Test
    void usesCurrentReadAfterUniqueKeyRaceToReplayCommittedResult() {
        TenantContext.set(new TenantPrincipal(801L, 100L));
        AdmissionIdempotencyRepository raceRepository = org.mockito.Mockito.mock(AdmissionIdempotencyRepository.class);
        AdmissionIdempotencyService raceService = new AdmissionIdempotencyService(raceRepository);
        Admission expected = admission(4L, AdmissionStatus.PENDING_CONFIRMATION);
        AdmissionIdempotencyRecord completed = new AdmissionIdempotencyRecord(
                1L, 100L, 801L, "ASSIGN_BED", "request-1", "fingerprint-1",
                AdmissionIdempotencyStatus.COMPLETED, expected, OffsetDateTime.parse("2026-07-29T08:00:00Z"), OffsetDateTime.parse("2026-07-29T08:00:01Z")
        );
        when(raceRepository.find(100L, 801L, "ASSIGN_BED", "request-1")).thenReturn(Optional.empty());
        when(raceRepository.begin(100L, 801L, "ASSIGN_BED", "request-1", "fingerprint-1")).thenReturn(false);
        when(raceRepository.findForUpdate(100L, 801L, "ASSIGN_BED", "request-1")).thenReturn(Optional.of(completed));

        Admission replay = raceService.execute("ASSIGN_BED", "request-1", "fingerprint-1", () -> admission(99L, AdmissionStatus.ADMITTED));

        assertEquals(expected, replay);
        verify(raceRepository).findForUpdate(100L, 801L, "ASSIGN_BED", "request-1");
    }

    @Test
    void rejectsBlankOrOversizedIdempotencyKey() {
        TenantContext.set(new TenantPrincipal(801L, 100L));

        assertThrows(InvalidIdempotencyKeyException.class, () -> service.execute(
                "ASSIGN_BED", " ", "fingerprint-1", () -> admission(4L, AdmissionStatus.PENDING_CONFIRMATION)
        ));
        assertThrows(InvalidIdempotencyKeyException.class, () -> service.execute(
                "ASSIGN_BED", "x".repeat(129), "fingerprint-1", () -> admission(4L, AdmissionStatus.PENDING_CONFIRMATION)
        ));
    }

    @Test
    void declaresTransactionForIdempotencyAndBusinessAction() throws NoSuchMethodException {
        Method execute = AdmissionIdempotencyService.class.getMethod("execute", String.class, String.class, String.class, java.util.function.Supplier.class);
        assertNotNull(execute.getAnnotation(Transactional.class));
    }

    private Admission admission(long version, AdmissionStatus status) {
        return new Admission(3L, 100L, 20L, 7L, null, status, version, OffsetDateTime.parse("2026-07-29T08:00:00Z"));
    }

    private static class InMemoryAdmissionIdempotencyRepository implements AdmissionIdempotencyRepository {
        private final Map<Scope, AdmissionIdempotencyRecord> records = new HashMap<>();

        @Override
        public Optional<AdmissionIdempotencyRecord> find(Long tenantId, Long actorId, String operation, String idempotencyKey) {
            return Optional.ofNullable(records.get(new Scope(tenantId, actorId, operation, idempotencyKey)));
        }

        @Override
        public Optional<AdmissionIdempotencyRecord> findForUpdate(Long tenantId, Long actorId, String operation, String idempotencyKey) {
            return find(tenantId, actorId, operation, idempotencyKey);
        }

        @Override
        public boolean begin(Long tenantId, Long actorId, String operation, String idempotencyKey, String requestFingerprint) {
            Scope scope = new Scope(tenantId, actorId, operation, idempotencyKey);
            if (records.containsKey(scope)) {
                return false;
            }
            records.put(scope, new AdmissionIdempotencyRecord(
                    1L, tenantId, actorId, operation, idempotencyKey, requestFingerprint,
                    AdmissionIdempotencyStatus.IN_PROGRESS, null, OffsetDateTime.parse("2026-07-29T08:00:00Z"), null
            ));
            return true;
        }

        @Override
        public void complete(Long tenantId, Long actorId, String operation, String idempotencyKey, Admission result) {
            Scope scope = new Scope(tenantId, actorId, operation, idempotencyKey);
            AdmissionIdempotencyRecord current = records.get(scope);
            records.put(scope, new AdmissionIdempotencyRecord(
                    current.id(), current.tenantId(), current.actorId(), current.operation(), current.idempotencyKey(), current.requestFingerprint(),
                    AdmissionIdempotencyStatus.COMPLETED, result, current.createdAt(), OffsetDateTime.parse("2026-07-29T08:00:01Z")
            ));
        }

        private record Scope(Long tenantId, Long actorId, String operation, String idempotencyKey) {
        }
    }
}
