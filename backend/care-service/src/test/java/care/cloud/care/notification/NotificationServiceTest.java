package care.cloud.care.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NotificationServiceTest {
    private final CareNotificationRepository repository = Mockito.mock(CareNotificationRepository.class);
    private final NotificationService service = new NotificationService(repository);

    @AfterEach
    void clearTenantContext() { TenantContext.clear(); }

    @Test
    void listUsesTrustedTenantAndCurrentMember() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(repository.findVisible(12L, 45L, true)).thenReturn(List.of());

        assertEquals(List.of(), service.list(true));
        verify(repository).findVisible(12L, 45L, true);
    }

    @Test
    void broadcastUsesTheOrganizationRecipientAndDedupeKey() {
        service.publishBroadcast(12L, NotificationCategory.TASK_OVERDUE, NotificationPriority.HIGH,
                "护理任务已逾期", "CARE_TASK", 81L, 91L, "TASK_OVERDUE:81");

        org.mockito.ArgumentCaptor<CareNotification> captor = org.mockito.ArgumentCaptor.forClass(CareNotification.class);
        verify(repository).createIfAbsent(captor.capture());
        assertEquals(NotificationService.ORGANIZATION_BROADCAST, captor.getValue().recipientId());
        assertEquals("TASK_OVERDUE:81", captor.getValue().dedupeKey());
    }
}
