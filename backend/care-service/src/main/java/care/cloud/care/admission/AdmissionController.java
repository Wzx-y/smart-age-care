package care.cloud.care.admission;

import care.cloud.care.shared.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionController {
    private final BedAssignmentService bedAssignmentService;
    private final AdmissionCreationService admissionCreationService;
    private final AdmissionConfirmationService admissionConfirmationService;
    private final AdmissionDischargeService admissionDischargeService;
    private final AdmissionCancellationService admissionCancellationService;
    private final AdmissionAssessmentService admissionAssessmentService;
    private final AdmissionBedTransferService admissionBedTransferService;
    private final AdmissionQueryService admissionQueryService;
    private final AdmissionIdempotencyService admissionIdempotencyService;

    public AdmissionController(
            BedAssignmentService bedAssignmentService,
            AdmissionCreationService admissionCreationService,
            AdmissionConfirmationService admissionConfirmationService,
            AdmissionDischargeService admissionDischargeService,
            AdmissionCancellationService admissionCancellationService,
            AdmissionAssessmentService admissionAssessmentService,
            AdmissionBedTransferService admissionBedTransferService,
            AdmissionQueryService admissionQueryService,
            AdmissionIdempotencyService admissionIdempotencyService
    ) {
        this.bedAssignmentService = bedAssignmentService;
        this.admissionCreationService = admissionCreationService;
        this.admissionConfirmationService = admissionConfirmationService;
        this.admissionDischargeService = admissionDischargeService;
        this.admissionCancellationService = admissionCancellationService;
        this.admissionAssessmentService = admissionAssessmentService;
        this.admissionBedTransferService = admissionBedTransferService;
        this.admissionQueryService = admissionQueryService;
        this.admissionIdempotencyService = admissionIdempotencyService;
    }

    @GetMapping
    public ApiResponse<List<AdmissionSummary>> list() {
        return ApiResponse.success(admissionQueryService.list());
    }

    @GetMapping("/{admissionId}/audit")
    public ApiResponse<List<AdmissionAuditEvent>> audit(@PathVariable Long admissionId) {
        return ApiResponse.success(admissionQueryService.audit(admissionId));
    }

    @PostMapping
    public ApiResponse<Admission> create(@org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
                                         @Valid @RequestBody CreateAdmissionRequest request) {
        return ApiResponse.success(admissionIdempotencyService.execute(
                "CREATE_ADMISSION", idempotencyKey, AdmissionRequestFingerprint.forCreate(request),
                () -> admissionCreationService.create(request)
        ));
    }

    @PostMapping("/{admissionId}/assign-bed")
    public ApiResponse<Admission> assignBed(
            @PathVariable Long admissionId,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AssignBedRequest request
    ) {
        return ApiResponse.success(admissionIdempotencyService.execute(
                "ASSIGN_BED", idempotencyKey, AdmissionRequestFingerprint.forAssignBed(admissionId, request),
                () -> bedAssignmentService.assign(admissionId, request)
        ));
    }

    @PostMapping("/{admissionId}/assessment")
    public ApiResponse<Admission> decideAssessment(@PathVariable Long admissionId,
                                                   @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
                                                   @Valid @RequestBody DecideAdmissionAssessmentRequest request) {
        return ApiResponse.success(admissionIdempotencyService.execute(
                "DECIDE_ADMISSION_ASSESSMENT", idempotencyKey, AdmissionRequestFingerprint.forAssessment(admissionId, request),
                () -> admissionAssessmentService.decide(admissionId, request)
        ));
    }

    @PostMapping("/{admissionId}/transfer-bed")
    public ApiResponse<Admission> transferBed(@PathVariable Long admissionId,
                                              @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
                                              @Valid @RequestBody TransferBedRequest request) {
        return ApiResponse.success(admissionIdempotencyService.execute(
                "TRANSFER_ADMISSION_BED", idempotencyKey, AdmissionRequestFingerprint.forTransfer(admissionId, request),
                () -> admissionBedTransferService.transfer(admissionId, request)
        ));
    }

    @PostMapping("/{admissionId}/confirm")
    public ApiResponse<Admission> confirm(
            @PathVariable Long admissionId,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ConfirmAdmissionRequest request
    ) {
        return ApiResponse.success(admissionIdempotencyService.execute(
                "CONFIRM_ADMISSION", idempotencyKey, AdmissionRequestFingerprint.forConfirm(admissionId, request),
                () -> admissionConfirmationService.confirm(admissionId, request)
        ));
    }

    @PostMapping("/{admissionId}/discharge")
    public ApiResponse<Admission> discharge(
            @PathVariable Long admissionId,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DischargeAdmissionRequest request
    ) {
        return ApiResponse.success(admissionIdempotencyService.execute(
                "DISCHARGE_ADMISSION", idempotencyKey, AdmissionRequestFingerprint.forDischarge(admissionId, request),
                () -> admissionDischargeService.discharge(admissionId, request)
        ));
    }

    @PostMapping("/{admissionId}/cancel")
    public ApiResponse<Admission> cancel(@PathVariable Long admissionId,
                                         @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String idempotencyKey,
                                         @Valid @RequestBody CancelAdmissionRequest request) {
        return ApiResponse.success(admissionIdempotencyService.execute(
                "CANCEL_ADMISSION", idempotencyKey, AdmissionRequestFingerprint.forCancel(admissionId, request),
                () -> admissionCancellationService.cancel(admissionId, request)
        ));
    }
}
