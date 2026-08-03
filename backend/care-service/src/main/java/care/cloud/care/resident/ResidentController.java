package care.cloud.care.resident;

import care.cloud.care.shared.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/residents")
public class ResidentController {
    private final ResidentService residentService;
    private final Resident360Service resident360Service;

    public ResidentController(ResidentService residentService, Resident360Service resident360Service) {
        this.residentService = residentService;
        this.resident360Service = resident360Service;
    }

    @GetMapping
    public ApiResponse<List<Resident>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(residentService.list(keyword));
    }

    @GetMapping("/{residentId}")
    public ApiResponse<Resident> get(@PathVariable Long residentId) {
        return ApiResponse.success(residentService.get(residentId));
    }

    @GetMapping("/{residentId}/360")
    public ApiResponse<Resident360View> get360(@PathVariable Long residentId) {
        return ApiResponse.success(resident360Service.get(residentId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Resident> create(@Valid @RequestBody CreateResidentRequest request) {
        return ApiResponse.success(residentService.create(request));
    }

    @PatchMapping("/{residentId}")
    public ApiResponse<Resident> update(@PathVariable Long residentId, @Valid @RequestBody UpdateResidentRequest request) {
        return ApiResponse.success(residentService.update(residentId, request));
    }

    @GetMapping("/{residentId}/health-profile")
    public ApiResponse<ResidentHealthProfile> getHealthProfile(@PathVariable Long residentId) {
        return ApiResponse.success(residentService.getHealthProfile(residentId));
    }

    @PatchMapping("/{residentId}/health-profile")
    public ApiResponse<ResidentHealthProfile> updateHealthProfile(@PathVariable Long residentId, @Valid @RequestBody UpdateResidentHealthProfileRequest request) {
        return ApiResponse.success(residentService.updateHealthProfile(residentId, request));
    }

    @DeleteMapping("/{residentId}")
    public ApiResponse<Void> archive(@PathVariable Long residentId) {
        residentService.archive(residentId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{residentId}/contacts")
    public ApiResponse<List<ResidentContact>> listContacts(@PathVariable Long residentId) {
        return ApiResponse.success(residentService.listContacts(residentId));
    }

    @PostMapping("/{residentId}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResidentContact> createContact(@PathVariable Long residentId, @Valid @RequestBody CreateResidentContactRequest request) {
        return ApiResponse.success(residentService.createContact(residentId, request));
    }

    @PatchMapping("/{residentId}/contacts/{contactId}")
    public ApiResponse<ResidentContact> updateContact(@PathVariable Long residentId, @PathVariable Long contactId,
                                                      @Valid @RequestBody UpdateResidentContactRequest request) {
        return ApiResponse.success(residentService.updateContact(residentId, contactId, request));
    }

    @DeleteMapping("/{residentId}/contacts/{contactId}")
    public ApiResponse<Void> deleteContact(@PathVariable Long residentId, @PathVariable Long contactId,
                                           @RequestParam long version) {
        residentService.deleteContact(residentId, contactId, version);
        return ApiResponse.success(null);
    }

    @GetMapping("/{residentId}/assessments")
    public ApiResponse<List<ResidentAssessment>> listAssessments(@PathVariable Long residentId) {
        return ApiResponse.success(residentService.listAssessments(residentId));
    }

    @PostMapping("/{residentId}/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResidentAssessment> createAssessment(@PathVariable Long residentId, @Valid @RequestBody CreateResidentAssessmentRequest request) {
        return ApiResponse.success(residentService.createAssessment(residentId, request));
    }

    @PatchMapping("/{residentId}/assessments/{assessmentId}")
    public ApiResponse<ResidentAssessment> updateAssessment(@PathVariable Long residentId, @PathVariable Long assessmentId,
                                                            @Valid @RequestBody UpdateResidentAssessmentRequest request) {
        return ApiResponse.success(residentService.updateAssessment(residentId, assessmentId, request));
    }

    @DeleteMapping("/{residentId}/assessments/{assessmentId}")
    public ApiResponse<Void> deleteAssessment(@PathVariable Long residentId, @PathVariable Long assessmentId,
                                              @RequestParam long version) {
        residentService.deleteAssessment(residentId, assessmentId, version);
        return ApiResponse.success(null);
    }

    @GetMapping("/{residentId}/attachments")
    public ApiResponse<List<ResidentAttachment>> listAttachments(@PathVariable Long residentId) {
        return ApiResponse.success(residentService.listAttachments(residentId));
    }

    @PostMapping("/{residentId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResidentAttachment> prepareAttachment(@PathVariable Long residentId, @Valid @RequestBody CreateResidentAttachmentRequest request) {
        return ApiResponse.success(residentService.prepareAttachment(residentId, request));
    }

    @PostMapping("/{residentId}/attachments/{attachmentId}/upload-url")
    public ApiResponse<ResidentAttachmentUploadTarget> createAttachmentUploadTarget(@PathVariable Long residentId, @PathVariable Long attachmentId) {
        return ApiResponse.success(residentService.createAttachmentUploadTarget(residentId, attachmentId));
    }

    @PostMapping("/{residentId}/attachments/{attachmentId}/complete")
    public ApiResponse<ResidentAttachment> completeAttachmentUpload(@PathVariable Long residentId, @PathVariable Long attachmentId) {
        return ApiResponse.success(residentService.completeAttachmentUpload(residentId, attachmentId));
    }

    @PostMapping("/{residentId}/attachments/{attachmentId}/access-url")
    public ApiResponse<ResidentAttachmentAccessTarget> createAttachmentAccessTarget(
            @PathVariable Long residentId, @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "true") boolean inline
    ) {
        return ApiResponse.success(residentService.createAttachmentAccessTarget(residentId, attachmentId, inline));
    }
}
