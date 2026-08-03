package care.cloud.care.care;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.notification.NotificationService;
import care.cloud.care.security.TenantPrincipal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CareTaskServiceTest {
    private final CarePlanRepository carePlanRepository = Mockito.mock(CarePlanRepository.class);
    private final CareTaskRepository careTaskRepository = Mockito.mock(CareTaskRepository.class);
    private final CareAuditRepository careAuditRepository = Mockito.mock(CareAuditRepository.class);
    private final NotificationService notificationService = Mockito.mock(NotificationService.class);
    private final CareTaskService service = new CareTaskService(carePlanRepository, careTaskRepository, careAuditRepository, notificationService);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void createUsesPublishedPlanInCurrentTenantAndDerivesResident() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CarePlan plan = plan();
        when(carePlanRepository.findByIdAndTenantId(71L, 12L)).thenReturn(Optional.of(plan));
        when(careTaskRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        OffsetDateTime scheduledAt = OffsetDateTime.of(2026, 8, 2, 8, 0, 0, 0, ZoneOffset.UTC);

        CareTask created = service.create(new CreateCareTaskRequest(71L, "晨间翻身", scheduledAt, 52L));

        ArgumentCaptor<CareTask> captor = ArgumentCaptor.forClass(CareTask.class);
        verify(carePlanRepository).lockResident(12L, 91L);
        verify(careTaskRepository).create(captor.capture());
        assertEquals(91L, captor.getValue().residentId());
        assertEquals(CareTaskStatus.PENDING, created.status());
    }

    @Test
    void completeWritesServiceRecordInTheSameOperation() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CareTask task = new CareTask(81L, 12L, 71L, 91L, "晨间翻身",
                OffsetDateTime.now(ZoneOffset.UTC), 52L, CareTaskStatus.PENDING, null, 4L);
        when(careTaskRepository.findByIdAndTenantId(81L, 12L)).thenReturn(Optional.of(task));
        when(careTaskRepository.complete(task, 45L)).thenReturn(true);

        CareTask completed = service.complete(81L, new CompleteCareTaskRequest(4L, "已完成并记录皮肤情况"));

        verify(careTaskRepository).createServiceRecord(12L, 81L, 45L, "已完成并记录皮肤情况");
        assertEquals(CareTaskStatus.COMPLETED, completed.status());
        assertEquals(5L, completed.version());
    }

    @Test
    void startsPendingTaskAndWritesMinimalAuditEvent() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CareTask task = new CareTask(81L, 12L, 71L, 91L, "晨间翻身",
                OffsetDateTime.now(ZoneOffset.UTC), 45L, CareTaskStatus.PENDING, null, 4L);
        when(careTaskRepository.findByIdAndTenantId(81L, 12L)).thenReturn(Optional.of(task));
        when(careTaskRepository.start(task, 45L)).thenReturn(true);

        CareTask started = service.start(81L, new StartCareTaskRequest(4L));

        assertEquals(CareTaskStatus.IN_PROGRESS, started.status());
        assertEquals(5L, started.version());
        verify(careAuditRepository).recordTaskAction(12L, 45L, "CARE_TASK_STARTED", task);
    }

    @Test
    void exceptionCreatesAnOpenFollowUpForTheSameResident() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        CareTask task = new CareTask(81L, 12L, 71L, 91L, "晨间翻身",
                OffsetDateTime.now(ZoneOffset.UTC), 52L, CareTaskStatus.PENDING, null, 4L);
        when(careTaskRepository.findByIdAndTenantId(81L, 12L)).thenReturn(Optional.of(task));
        when(careTaskRepository.markException(task, "长者拒绝服务")).thenReturn(true);

        CareTask exception = service.markException(81L, new MarkCareTaskExceptionRequest(4L, "长者拒绝服务"));

        verify(careTaskRepository).createExceptionFollowUp(eq(12L), eq(task), eq(45L), eq("长者拒绝服务"));
        assertEquals(CareTaskStatus.EXCEPTION, exception.status());
    }

    private CarePlan plan() {
        return new CarePlan(71L, 12L, 91L, null, "晨间照护", "每日 08:00", 45L,
                CarePlanStatus.PUBLISHED, 3L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
    }
}
