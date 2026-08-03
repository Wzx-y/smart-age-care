package care.cloud.care.notification;

import care.cloud.care.care.CareTaskRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OverdueCareTaskNotificationScheduler {
    private final CareTaskRepository taskRepository;
    private final NotificationService notificationService;

    public OverdueCareTaskNotificationScheduler(CareTaskRepository taskRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${notifications.overdue-scan-ms:60000}")
    public void notifyOverdueTasks() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (var task : taskRepository.findUnfinishedScheduledBefore(now)) {
            notificationService.publishBroadcast(task.tenantId(), NotificationCategory.TASK_OVERDUE, NotificationPriority.HIGH,
                    "护理任务已逾期", "CARE_TASK", task.id(), task.residentId(), "TASK_OVERDUE:" + task.id());
        }
    }
}
