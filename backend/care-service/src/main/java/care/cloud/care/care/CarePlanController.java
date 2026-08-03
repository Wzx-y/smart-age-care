package care.cloud.care.care;

import care.cloud.care.shared.ApiResponse;
import care.cloud.care.security.CareAuthorizationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/care-plans")
public class CarePlanController {
    private final CarePlanService carePlanService;
    private final CareIdempotencyService careIdempotencyService;
    private final CareAuthorizationService authorizationService;

    public CarePlanController(CarePlanService carePlanService, CareIdempotencyService careIdempotencyService,
                              CareAuthorizationService authorizationService) {
        this.carePlanService = carePlanService;
        this.careIdempotencyService = careIdempotencyService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<List<CarePlan>> list() {
        return ApiResponse.success(carePlanService.list());
    }

    @PostMapping
    public ApiResponse<CarePlan> create(
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateCarePlanRequest request
    ) {
        authorizationService.requireCurrent(CareAuthorizationService.PLAN_MANAGE);
        return ApiResponse.success(careIdempotencyService.execute(
                "CREATE_CARE_PLAN", idempotencyKey, CareRequestFingerprint.forCreatePlan(request),
                () -> carePlanService.create(request), CareWriteResult::plan, carePlanService::replay
        ));
    }

    @PostMapping("/{planId}/publish")
    public ApiResponse<CarePlan> publish(
            @PathVariable Long planId,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PublishCarePlanRequest request
    ) {
        authorizationService.requireCurrent(CareAuthorizationService.PLAN_PUBLISH);
        return ApiResponse.success(careIdempotencyService.execute(
                "PUBLISH_CARE_PLAN", idempotencyKey, CareRequestFingerprint.forPublishPlan(planId, request),
                () -> carePlanService.publish(planId, request), CareWriteResult::plan, carePlanService::replay
        ));
    }

    @PatchMapping("/{planId}")
    public ApiResponse<CarePlan> update(@PathVariable Long planId, @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
                                        @Valid @RequestBody UpdateCarePlanRequest request) {
        authorizationService.requireCurrent(CareAuthorizationService.PLAN_MANAGE);
        return ApiResponse.success(careIdempotencyService.execute("UPDATE_CARE_PLAN", idempotencyKey,
                CareRequestFingerprint.forUpdatePlan(planId, request), () -> carePlanService.update(planId, request), CareWriteResult::plan, carePlanService::replay));
    }

    @PostMapping("/{planId}/cancel")
    public ApiResponse<CarePlan> cancel(@PathVariable Long planId, @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
                                        @Valid @RequestBody ChangeCarePlanStatusRequest request) {
        authorizationService.requireCurrent(CareAuthorizationService.PLAN_MANAGE);
        return ApiResponse.success(careIdempotencyService.execute("CANCEL_CARE_PLAN", idempotencyKey,
                CareRequestFingerprint.forCancelPlan(planId, request), () -> carePlanService.cancel(planId, request), CareWriteResult::plan, carePlanService::replay));
    }
}
