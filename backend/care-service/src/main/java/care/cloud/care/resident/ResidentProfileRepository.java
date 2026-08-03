package care.cloud.care.resident;

import java.util.List;

public interface ResidentProfileRepository {
    java.util.Optional<ResidentHealthProfile> findHealthProfile(Long tenantId, Long residentId);

    ResidentHealthProfile saveHealthProfile(Long tenantId, Long residentId, Long userId, UpdateResidentHealthProfileRequest request);

    List<ResidentContact> findContacts(Long tenantId, Long residentId);

    void clearPrimaryContact(Long tenantId, Long residentId);

    void clearPrimaryContactExcept(Long tenantId, Long residentId, Long contactId);

    ResidentContact saveContact(Long tenantId, Long residentId, CreateResidentContactRequest request);

    boolean updateContact(Long tenantId, Long residentId, Long contactId, UpdateResidentContactRequest request);

    boolean deleteContact(Long tenantId, Long residentId, Long contactId, long version);

    List<ResidentAssessment> findAssessments(Long tenantId, Long residentId);

    ResidentAssessment saveAssessment(Long tenantId, Long residentId, Long assessorId, CreateResidentAssessmentRequest request);

    boolean updateAssessment(Long tenantId, Long residentId, Long assessmentId, UpdateResidentAssessmentRequest request);

    boolean isAssessmentReferenced(Long tenantId, Long residentId, Long assessmentId);

    boolean deleteAssessment(Long tenantId, Long residentId, Long assessmentId, long version);

    List<ResidentAttachment> findAttachments(Long tenantId, Long residentId);

    java.util.Optional<ResidentAttachment> findAttachmentById(Long tenantId, Long residentId, Long attachmentId);

    ResidentAttachment saveAttachment(Long tenantId, Long residentId, Long userId, CreateResidentAttachmentRequest request, String storageKey);

    boolean markAttachmentUploaded(Long tenantId, Long residentId, Long attachmentId);
}
