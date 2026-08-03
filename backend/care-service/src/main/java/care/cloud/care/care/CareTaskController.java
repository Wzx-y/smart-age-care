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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/care-tasks")
public class CareTaskController {
    private final CareTaskService careTaskService;
    private final CareIdempotencyService careIdempotencyService;
    private final CareAuthorizationService authorizationService;

    public CareTaskController(CareTaskService careTaskService, CareIdempotencyService careIdempotencyService,
                              CareAuthorizationService authorizationService) {
        this.careTaskService = careTaskService;
        this.careIdempotencyService = careIdempotencyService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<List<CareTask>> list() {
        return ApiResponse.success(careTaskService.list());
    }

    @PostMapping
    public ApiResponse<CareTask> create(
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateCareTaskRequest request
    ) {
        authorizationService.requireCurrent(CareAuthorizationService.TASK_MANAGE);
        authorizationService.requireMember(request.assigneeId(), CareAuthorizationService.TASK_EXECUTE);
        return ApiResponse.success(careIdempotencyService.execute(
                "CREATE_CARE_TASK", idempotencyKey, CareRequestFingerprint.forCreateTask(request),
                () -> careTaskService.create(request), CareWriteResult::task, careTaskService::replay
        ));
    }

    @PostMapping("/{taskId}/complete")
    public ApiResponse<CareTask> complete(
            @PathVariable Long taskId,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CompleteCareTaskRequest request
    ) {
        authorizationService.requireAssignedExecutor(taskId);
        return ApiResponse.success(careIdempotencyService.execute(
                "COMPLETE_CARE_TASK", idempotencyKey, CareRequestFingerprint.forCompleteTask(taskId, request),
                () -> careTaskService.complete(taskId, request), CareWriteResult::task, careTaskService::replay
        ));
    }

    @PostMapping("/{taskId}/start")
    public ApiResponse<CareTask> start(
            @PathVariable Long taskId,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody StartCareTaskRequest request
    ) {
        authorizationService.requireAssignedExecutor(taskId);
        return ApiResponse.success(careIdempotencyService.execute(
                "START_CARE_TASK", idempotencyKey, CareRequestFingerprint.forStartTask(taskId, request),
                () -> careTaskService.start(taskId, request), CareWriteResult::task, careTaskService::replay
        ));
    }

    @PostMapping("/{taskId}/exception")
    public ApiResponse<CareTask> markException(
            @PathVariable Long taskId,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody MarkCareTaskExceptionRequest request
    ) {
        authorizationService.requireAssignedExecutor(taskId);
        return ApiResponse.success(careIdempotencyService.execute(
                "MARK_CARE_TASK_EXCEPTION", idempotencyKey, CareRequestFingerprint.forTaskException(taskId, request),
                () -> careTaskService.markException(taskId, request), CareWriteResult::task, careTaskService::replay
        ));
    }
}
