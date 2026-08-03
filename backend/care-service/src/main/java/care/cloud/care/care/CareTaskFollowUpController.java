package care.cloud.care.care;

import care.cloud.care.shared.ApiResponse;
import care.cloud.care.security.CareAuthorizationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/care-task-follow-ups")
public class CareTaskFollowUpController {
    private final CareTaskFollowUpService service;
    private final CareIdempotencyService idempotencyService;
    private final CareAuthorizationService authorizationService;

    public CareTaskFollowUpController(CareTaskFollowUpService service, CareIdempotencyService idempotencyService,
                                      CareAuthorizationService authorizationService) {
        this.service = service;
        this.idempotencyService = idempotencyService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<List<CareTaskFollowUp>> list(@RequestParam(required = false) CareTaskFollowUpStatus status) {
        return ApiResponse.success(service.list(status));
    }

    @PostMapping("/{followUpId}/resolve")
    public ApiResponse<CareTaskFollowUp> resolve(
            @PathVariable Long followUpId,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ResolveCareTaskFollowUpRequest request
    ) {
        authorizationService.requireCurrent(CareAuthorizationService.FOLLOW_UP_MANAGE);
        return ApiResponse.success(idempotencyService.execute(
                "RESOLVE_CARE_TASK_FOLLOW_UP", idempotencyKey, CareRequestFingerprint.forResolveFollowUp(followUpId, request),
                () -> service.resolve(followUpId, request), CareWriteResult::followUp, service::replay
        ));
    }
}
