package care.cloud.care.audit;

import care.cloud.care.security.CareAuthorizationService;
import care.cloud.care.shared.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditEventController {
    private final AuditEventService service;
    private final CareAuthorizationService authorization;

    public AuditEventController(AuditEventService service, CareAuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping
    public ApiResponse<List<AuditEvent>> list(@RequestParam(required = false) String resourceType,
                                              @RequestParam(required = false) String action,
                                              @RequestParam(required = false) Long actorId,
                                              @RequestParam(defaultValue = "50") int limit) {
        authorization.requireCurrent(CareAuthorizationService.AUDIT_VIEW);
        return ApiResponse.success(service.list(resourceType, action, actorId, limit));
    }
}
