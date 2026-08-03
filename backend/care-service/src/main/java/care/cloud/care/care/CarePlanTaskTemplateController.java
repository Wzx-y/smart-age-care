package care.cloud.care.care;

import care.cloud.care.shared.ApiResponse;
import care.cloud.care.security.CareAuthorizationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/care-plan-task-templates")
public class CarePlanTaskTemplateController {
    private final CarePlanTaskTemplateService service;
    private final CareIdempotencyService idempotencyService;
    private final CareAuthorizationService authorizationService;

    public CarePlanTaskTemplateController(CarePlanTaskTemplateService service, CareIdempotencyService idempotencyService,
                                          CareAuthorizationService authorizationService) {
        this.service = service;
        this.idempotencyService = idempotencyService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<List<CarePlanTaskTemplate>> list(@RequestParam Long planId) {
        return ApiResponse.success(service.list(planId));
    }

    @PostMapping
    public ApiResponse<CarePlanTaskTemplate> create(
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateCarePlanTaskTemplateRequest request
    ) {
        authorizationService.requireCurrent(CareAuthorizationService.PLAN_MANAGE);
        authorizationService.requireMember(request.assigneeId(), CareAuthorizationService.TASK_EXECUTE);
        return ApiResponse.success(idempotencyService.execute("CREATE_CARE_PLAN_TASK_TEMPLATE", idempotencyKey,
                CareRequestFingerprint.forCreateTemplate(request), () -> service.create(request), CareWriteResult::template, service::replay));
    }

    @PatchMapping("/{templateId}")
    public ApiResponse<CarePlanTaskTemplate> update(@PathVariable Long templateId, @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
                                                     @Valid @RequestBody UpdateCarePlanTaskTemplateRequest request) {
        authorizationService.requireCurrent(CareAuthorizationService.PLAN_MANAGE);
        authorizationService.requireMember(request.assigneeId(), CareAuthorizationService.TASK_EXECUTE);
        return ApiResponse.success(idempotencyService.execute("UPDATE_CARE_PLAN_TASK_TEMPLATE", idempotencyKey,
                CareRequestFingerprint.forUpdateTemplate(templateId, request), () -> service.update(templateId, request), CareWriteResult::template, service::replay));
    }

    @PostMapping("/{templateId}/deactivate")
    public ApiResponse<CarePlanTaskTemplate> deactivate(@PathVariable Long templateId, @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
                                                         @Valid @RequestBody ChangeCarePlanTaskTemplateStatusRequest request) {
        authorizationService.requireCurrent(CareAuthorizationService.PLAN_MANAGE);
        return ApiResponse.success(idempotencyService.execute("DEACTIVATE_CARE_PLAN_TASK_TEMPLATE", idempotencyKey,
                CareRequestFingerprint.forDeactivateTemplate(templateId, request), () -> service.deactivate(templateId, request), CareWriteResult::template, service::replay));
    }
}
