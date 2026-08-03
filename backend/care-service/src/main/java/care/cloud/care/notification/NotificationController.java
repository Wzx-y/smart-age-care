package care.cloud.care.notification;

import care.cloud.care.security.CareAuthorizationService;
import care.cloud.care.shared.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    private final CareAuthorizationService authorization;

    public NotificationController(NotificationService service, CareAuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping
    public ApiResponse<List<CareNotification>> list(@RequestParam(defaultValue = "false") boolean unreadOnly) {
        authorization.requireCurrent(CareAuthorizationService.NOTIFICATION_VIEW);
        return ApiResponse.success(service.list(unreadOnly));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long notificationId) {
        authorization.requireCurrent(CareAuthorizationService.NOTIFICATION_VIEW);
        service.markRead(notificationId);
        return ApiResponse.success(null);
    }

    @PostMapping("/read-all")
    public ApiResponse<Integer> markAllRead() {
        authorization.requireCurrent(CareAuthorizationService.NOTIFICATION_VIEW);
        return ApiResponse.success(service.markAllRead());
    }
}
