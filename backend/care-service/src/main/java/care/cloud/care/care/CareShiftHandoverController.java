package care.cloud.care.care;

import care.cloud.care.security.CareAuthorizationService;
import care.cloud.care.shared.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/care-shift-handovers")
public class CareShiftHandoverController {
    private final CareShiftHandoverService service;
    private final CareIdempotencyService idempotencyService;
    private final CareAuthorizationService authorizationService;

    public CareShiftHandoverController(CareShiftHandoverService service, CareIdempotencyService idempotencyService,
                                       CareAuthorizationService authorizationService) {
        this.service = service;
        this.idempotencyService = idempotencyService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<List<CareShiftHandover>> list() {
        return ApiResponse.success(service.list());
    }

    @PostMapping
    public ApiResponse<CareShiftHandover> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                  @Valid @RequestBody CreateCareShiftHandoverRequest request) {
        authorizationService.requireCurrent(CareAuthorizationService.HANDOVER_MANAGE);
        authorizationService.requireMember(request.toUserId(), CareAuthorizationService.HANDOVER_RECEIVE);
        return ApiResponse.success(idempotencyService.execute("CREATE_CARE_SHIFT_HANDOVER", idempotencyKey,
                CareRequestFingerprint.forCreateShiftHandover(request), () -> service.create(request),
                CareWriteResult::handover, service::replay));
    }

    @PostMapping("/{handoverId}/submit")
    public ApiResponse<CareShiftHandover> submit(@PathVariable Long handoverId, @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                  @Valid @RequestBody SubmitCareShiftHandoverRequest request) {
        authorizationService.requireCurrent(CareAuthorizationService.HANDOVER_MANAGE);
        return ApiResponse.success(idempotencyService.execute("SUBMIT_CARE_SHIFT_HANDOVER", idempotencyKey,
                CareRequestFingerprint.forSubmitShiftHandover(handoverId, request), () -> service.submit(handoverId, request),
                CareWriteResult::handover, service::replay));
    }
}
