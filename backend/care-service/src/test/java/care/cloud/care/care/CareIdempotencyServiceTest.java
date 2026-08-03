package care.cloud.care.care;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CareIdempotencyServiceTest {
    private final CareIdempotencyRepository repository = Mockito.mock(CareIdempotencyRepository.class);
    private final CareIdempotencyService service = new CareIdempotencyService(repository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void firstWritePersistsAReplayableCareResult() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CareTask result = task();
        when(repository.find(12L, 45L, "COMPLETE_CARE_TASK", "key-1")).thenReturn(Optional.empty());
        when(repository.begin(12L, 45L, "COMPLETE_CARE_TASK", "key-1", "fingerprint")).thenReturn(true);

        CareTask completed = service.execute("COMPLETE_CARE_TASK", "key-1", "fingerprint", () -> result,
                CareWriteResult::task, ignored -> null);

        verify(repository).complete(eq(12L), eq(45L), eq("COMPLETE_CARE_TASK"), eq("key-1"), any(CareWriteResult.class));
        assertEquals(result, completed);
    }

    @Test
    void completedSameRequestReplaysWithoutExecutingTheActionAgain() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CareWriteResult stored = new CareWriteResult("TASK", 81L, "COMPLETED", 5L, null);
        CareIdempotencyRecord record = new CareIdempotencyRecord(1L, 12L, 45L, "COMPLETE_CARE_TASK", "key-1",
                "fingerprint", CareIdempotencyStatus.COMPLETED, stored, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
        when(repository.find(12L, 45L, "COMPLETE_CARE_TASK", "key-1")).thenReturn(Optional.of(record));

        CareTask replayed = service.execute("COMPLETE_CARE_TASK", "key-1", "fingerprint",
                () -> { throw new AssertionError("action should not execute"); }, CareWriteResult::task, ignored -> task());

        assertEquals(81L, replayed.id());
    }

    private CareTask task() {
        return new CareTask(81L, 12L, 71L, 91L, "晨间翻身", OffsetDateTime.now(ZoneOffset.UTC), 52L,
                CareTaskStatus.COMPLETED, null, 5L);
    }
}
