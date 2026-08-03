package care.cloud.care.care;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import care.cloud.care.notification.NotificationService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CareShiftHandoverServiceTest {
    private final CareShiftHandoverRepository handoverRepository = Mockito.mock(CareShiftHandoverRepository.class);
    private final CareTaskRepository taskRepository = Mockito.mock(CareTaskRepository.class);
    private final CareTaskFollowUpRepository followUpRepository = Mockito.mock(CareTaskFollowUpRepository.class);
    private final NotificationService notificationService = Mockito.mock(NotificationService.class);
    private final CareShiftHandoverService service = new CareShiftHandoverService(handoverRepository, taskRepository, followUpRepository, notificationService);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void createUsesCurrentTenantSenderAndOnlyClosedTaskStates() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CreateCareShiftHandoverRequest request = new CreateCareShiftHandoverRequest(LocalDate.of(2026, 8, 2), "DAY", 52L,
                "Medication reminder completed", List.of(81L, 82L));
        when(handoverRepository.findByShiftAndFromUser(12L, request.shiftDate(), "DAY", 45L)).thenReturn(Optional.empty());
        when(taskRepository.findByIdsAndTenantId(List.of(81L, 82L), 12L)).thenReturn(List.of(task(81L, CareTaskStatus.COMPLETED), task(82L, CareTaskStatus.EXCEPTION)));
        CareShiftHandover saved = handover(31L, CareShiftHandoverStatus.DRAFT, 0L);
        when(handoverRepository.create(any())).thenReturn(saved);
        when(handoverRepository.findTaskIds(31L, 12L)).thenReturn(List.of(81L, 82L));

        CareShiftHandover created = service.create(request);

        ArgumentCaptor<CareShiftHandover> captor = ArgumentCaptor.forClass(CareShiftHandover.class);
        verify(handoverRepository).create(captor.capture());
        verify(handoverRepository).createItems(12L, 31L, List.of(81L, 82L));
        assertEquals(45L, captor.getValue().fromUserId());
        assertEquals(CareShiftHandoverStatus.DRAFT, created.status());
        assertEquals(List.of(81L, 82L), created.taskIds());
    }

    @Test
    void createRejectsTasksThatAreStillPending() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CreateCareShiftHandoverRequest request = new CreateCareShiftHandoverRequest(LocalDate.of(2026, 8, 2), "DAY", 52L,
                "Medication reminder", List.of(81L));
        when(handoverRepository.findByShiftAndFromUser(12L, request.shiftDate(), "DAY", 45L)).thenReturn(Optional.empty());
        when(taskRepository.findByIdsAndTenantId(List.of(81L), 12L)).thenReturn(List.of(task(81L, CareTaskStatus.PENDING)));

        assertThrows(CareShiftHandoverConflictException.class, () -> service.create(request));
    }

    @Test
    void createRejectsDuplicateTaskIdsBeforeAnyTaskLookup() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CreateCareShiftHandoverRequest request = new CreateCareShiftHandoverRequest(LocalDate.of(2026, 8, 2), "DAY", 52L,
                "Medication reminder", List.of(81L, 81L));

        assertThrows(IllegalArgumentException.class, () -> service.create(request));
    }

    @Test
    void submitRejectsOpenExceptionFollowUp() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CareShiftHandover handover = handover(31L, CareShiftHandoverStatus.DRAFT, 2L);
        when(handoverRepository.findByIdAndTenantId(31L, 12L)).thenReturn(Optional.of(handover));
        when(handoverRepository.findTaskIds(31L, 12L)).thenReturn(List.of(81L));
        when(followUpRepository.countOpenByTaskIdsAndTenantId(List.of(81L), 12L)).thenReturn(1L);

        assertThrows(CareShiftHandoverConflictException.class,
                () -> service.submit(31L, new SubmitCareShiftHandoverRequest(2L)));
    }

    @Test
    void submitUsesVersionAndReturnsSubmittedState() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CareShiftHandover handover = handover(31L, CareShiftHandoverStatus.DRAFT, 2L);
        when(handoverRepository.findByIdAndTenantId(31L, 12L)).thenReturn(Optional.of(handover));
        when(handoverRepository.findTaskIds(31L, 12L)).thenReturn(List.of(81L));
        when(followUpRepository.countOpenByTaskIdsAndTenantId(List.of(81L), 12L)).thenReturn(0L);
        when(handoverRepository.submit(handover)).thenReturn(true);

        CareShiftHandover submitted = service.submit(31L, new SubmitCareShiftHandoverRequest(2L));

        verify(handoverRepository).submit(handover);
        assertEquals(CareShiftHandoverStatus.SUBMITTED, submitted.status());
        assertEquals(3L, submitted.version());
    }

    private CareTask task(Long id, CareTaskStatus status) {
        return new CareTask(id, 12L, 71L, 91L, "Medication reminder", OffsetDateTime.now(ZoneOffset.UTC), 45L,
                status, status == CareTaskStatus.EXCEPTION ? "Refused" : null, 0L);
    }

    private CareShiftHandover handover(Long id, CareShiftHandoverStatus status, long version) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new CareShiftHandover(id, 12L, LocalDate.of(2026, 8, 2), "DAY", 45L, 52L, "Medication reminder",
                status, null, now, now, version, List.of());
    }
}
