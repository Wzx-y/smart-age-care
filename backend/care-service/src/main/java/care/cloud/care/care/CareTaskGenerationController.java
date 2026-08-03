package care.cloud.care.care;

import care.cloud.care.shared.ApiResponse;
import care.cloud.care.security.CareAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/care-task-generation-runs")
public class CareTaskGenerationController {
    private final CareTaskGenerationService service;
    private final CareIdempotencyService idempotencyService;
    private final CareAuthorizationService authorizationService;

    public CareTaskGenerationController(CareTaskGenerationService service, CareIdempotencyService idempotencyService,
                                        CareAuthorizationService authorizationService) {
        this.service = service;
        this.idempotencyService = idempotencyService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ApiResponse<CareTaskGenerationRun> generate(
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody GenerateCareTasksRequest request
    ) {
        authorizationService.requireCurrent(CareAuthorizationService.TASK_MANAGE);
        return ApiResponse.success(idempotencyService.execute("GENERATE_CARE_TASKS", idempotencyKey,
                CareRequestFingerprint.forGenerateTasks(request), () -> service.generate(request),
                CareWriteResult::generationRun, service::replay));
    }
}
