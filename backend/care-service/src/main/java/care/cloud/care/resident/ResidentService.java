package care.cloud.care.resident;

import care.cloud.care.security.TenantContext;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ResidentService {
    private final ResidentRepository residentRepository;
    private final ResidentProfileRepository residentProfileRepository;
    private final ResidentAttachmentStorage residentAttachmentStorage;
    @Autowired(required = false)
    private ResidentAuditWriter auditWriter;

    private void audit(String action, String targetType, Long targetId, Long residentId) {
        if (auditWriter == null) return;
        var principal = TenantContext.requireCurrent();
        auditWriter.write(principal.tenantId(), principal.userId(), action, targetType, targetId, residentId);
    }

    public ResidentService(ResidentRepository residentRepository, ResidentProfileRepository residentProfileRepository, ResidentAttachmentStorage residentAttachmentStorage) {
        this.residentRepository = residentRepository;
        this.residentProfileRepository = residentProfileRepository;
        this.residentAttachmentStorage = residentAttachmentStorage;
    }

    public Resident create(CreateResidentRequest request) {
        Long tenantId = TenantContext.requireCurrent().tenantId();
        OffsetDateTime now = OffsetDateTime.now();
        Resident created = residentRepository.save(new Resident(
                null,
                tenantId,
                request.name(),
                request.gender(),
                request.birthDate(),
                ResidentStatus.PENDING_ADMISSION,
                null,
                request.emergencyContactName(),
                request.emergencyContactPhone(),
                now,
                now
        ));
        audit("RESIDENT_CREATED", "RESIDENT", created.id(), created.id());
        return created;
    }

    public Resident get(Long residentId) {
        Long tenantId = TenantContext.requireCurrent().tenantId();
        return residentRepository.findByIdAndTenantId(residentId, tenantId)
                .orElseThrow(() -> new ResidentNotFoundException(residentId));
    }

    public Resident update(Long residentId, UpdateResidentRequest request) {
        Resident current = get(residentId);
        Resident updated = new Resident(
                current.id(),
                current.tenantId(),
                request.name(),
                request.gender(),
                request.birthDate(),
                current.status(),
                current.currentBedId(),
                request.emergencyContactName(),
                request.emergencyContactPhone(),
                current.createdAt(),
                OffsetDateTime.now()
        );
        if (!residentRepository.update(updated)) {
            throw new ResidentNotFoundException(residentId);
        }
        Resident result = get(residentId);
        audit("RESIDENT_UPDATED", "RESIDENT", result.id(), result.id());
        return result;
    }

    public void archive(Long residentId) {
        Resident resident = get(residentId);
        if (resident.status() != ResidentStatus.DISCHARGED && resident.status() != ResidentStatus.DECEASED) {
            throw new ResidentArchiveConflictException();
        }
        if (!residentRepository.archive(resident.id(), resident.tenantId(), OffsetDateTime.now())) {
            throw new ResidentArchiveConflictException();
        }
        audit("RESIDENT_ARCHIVED", "RESIDENT", resident.id(), resident.id());
    }

    public List<ResidentContact> listContacts(Long residentId) {
        Resident resident = get(residentId);
        return residentProfileRepository.findContacts(resident.tenantId(), resident.id());
    }

    @Transactional
    public ResidentContact createContact(Long residentId, CreateResidentContactRequest request) {
        Resident resident = get(residentId);
        if (request.primaryContact()) {
            residentProfileRepository.clearPrimaryContact(resident.tenantId(), resident.id());
        }
        ResidentContact contact = residentProfileRepository.saveContact(resident.tenantId(), resident.id(), request);
        audit("RESIDENT_CONTACT_CREATED", "RESIDENT_CONTACT", contact.id(), resident.id());
        return contact;
    }

    @Transactional
    public ResidentContact updateContact(Long residentId, Long contactId, UpdateResidentContactRequest request) {
        Resident resident = get(residentId);
        if (request.primaryContact()) {
            residentProfileRepository.clearPrimaryContactExcept(resident.tenantId(), resident.id(), contactId);
        }
        if (!residentProfileRepository.updateContact(resident.tenantId(), resident.id(), contactId, request)) {
            throw new ResidentProfileConflictException();
        }
        ResidentContact result = residentProfileRepository.findContacts(resident.tenantId(), resident.id()).stream()
                .filter(contact -> contact.id().equals(contactId))
                .findFirst().orElseThrow(() -> new ResidentNotFoundException(residentId));
        audit("RESIDENT_CONTACT_UPDATED", "RESIDENT_CONTACT", result.id(), resident.id());
        return result;
    }

    public void deleteContact(Long residentId, Long contactId, long version) {
        Resident resident = get(residentId);
        if (!residentProfileRepository.deleteContact(resident.tenantId(), resident.id(), contactId, version)) {
            throw new ResidentProfileConflictException();
        }
        audit("RESIDENT_CONTACT_DELETED", "RESIDENT_CONTACT", contactId, resident.id());
    }

    public List<ResidentAssessment> listAssessments(Long residentId) {
        Resident resident = get(residentId);
        return residentProfileRepository.findAssessments(resident.tenantId(), resident.id());
    }

    public ResidentAssessment createAssessment(Long residentId, CreateResidentAssessmentRequest request) {
        Resident resident = get(residentId);
        Long assessorId = TenantContext.requireCurrent().userId();
        ResidentAssessment assessment = residentProfileRepository.saveAssessment(resident.tenantId(), resident.id(), assessorId, request);
        audit("RESIDENT_ASSESSMENT_CREATED", "RESIDENT_ASSESSMENT", assessment.id(), resident.id());
        return assessment;
    }

    public ResidentAssessment updateAssessment(Long residentId, Long assessmentId, UpdateResidentAssessmentRequest request) {
        Resident resident = get(residentId);
        if (!residentProfileRepository.updateAssessment(resident.tenantId(), resident.id(), assessmentId, request)) {
            throw new ResidentProfileConflictException();
        }
        ResidentAssessment result = residentProfileRepository.findAssessments(resident.tenantId(), resident.id()).stream()
                .filter(assessment -> assessment.id().equals(assessmentId))
                .findFirst().orElseThrow(() -> new ResidentNotFoundException(residentId));
        audit("RESIDENT_ASSESSMENT_UPDATED", "RESIDENT_ASSESSMENT", result.id(), resident.id());
        return result;
    }

    public void deleteAssessment(Long residentId, Long assessmentId, long version) {
        Resident resident = get(residentId);
        if (residentProfileRepository.isAssessmentReferenced(resident.tenantId(), resident.id(), assessmentId)) {
            throw new ResidentProfileConflictException();
        }
        if (!residentProfileRepository.deleteAssessment(resident.tenantId(), resident.id(), assessmentId, version)) {
            throw new ResidentProfileConflictException();
        }
        audit("RESIDENT_ASSESSMENT_DELETED", "RESIDENT_ASSESSMENT", assessmentId, resident.id());
    }

    public List<ResidentAttachment> listAttachments(Long residentId) {
        Resident resident = get(residentId);
        return residentProfileRepository.findAttachments(resident.tenantId(), resident.id());
    }

    public ResidentHealthProfile getHealthProfile(Long residentId) {
        Resident resident = get(residentId);
        return residentProfileRepository.findHealthProfile(resident.tenantId(), resident.id())
                .orElse(new ResidentHealthProfile(resident.id(), null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    }

    public ResidentHealthProfile updateHealthProfile(Long residentId, UpdateResidentHealthProfileRequest request) {
        Resident resident = get(residentId);
        ResidentHealthProfile result = residentProfileRepository.saveHealthProfile(resident.tenantId(), resident.id(), TenantContext.requireCurrent().userId(), request);
        audit("RESIDENT_HEALTH_PROFILE_UPDATED", "RESIDENT_HEALTH_PROFILE", resident.id(), resident.id());
        return result;
    }

    public ResidentAttachment prepareAttachment(Long residentId, CreateResidentAttachmentRequest request) {
        Resident resident = get(residentId);
        String storageKey = "resident-attachments/" + resident.tenantId() + "/" + resident.id() + "/" + UUID.randomUUID();
        Long userId = TenantContext.requireCurrent().userId();
        ResidentAttachment result = residentProfileRepository.saveAttachment(resident.tenantId(), resident.id(), userId, request, storageKey);
        audit("RESIDENT_ATTACHMENT_PREPARED", "RESIDENT_ATTACHMENT", result.id(), resident.id());
        return result;
    }

    public ResidentAttachmentUploadTarget createAttachmentUploadTarget(Long residentId, Long attachmentId) {
        Resident resident = get(residentId);
        ResidentAttachment attachment = findAttachment(resident, attachmentId);
        if (!"PENDING_UPLOAD".equals(attachment.uploadStatus())) {
            throw new ResidentAttachmentUploadConflictException();
        }
        return residentAttachmentStorage.signUpload(attachment.storageKey(), attachment.contentType());
    }

    public ResidentAttachmentAccessTarget createAttachmentAccessTarget(Long residentId, Long attachmentId, boolean inline) {
        Resident resident = get(residentId);
        ResidentAttachment attachment = findAttachment(resident, attachmentId);
        if (!"UPLOADED".equals(attachment.uploadStatus())) throw new ResidentAttachmentUploadConflictException();
        audit(inline ? "RESIDENT_ATTACHMENT_PREVIEWED" : "RESIDENT_ATTACHMENT_DOWNLOADED", "RESIDENT_ATTACHMENT", attachment.id(), resident.id());
        return residentAttachmentStorage.signDownload(attachment.storageKey(), inline);
    }

    @Transactional
    public ResidentAttachment completeAttachmentUpload(Long residentId, Long attachmentId) {
        Resident resident = get(residentId);
        ResidentAttachment attachment = findAttachment(resident, attachmentId);
        if ("UPLOADED".equals(attachment.uploadStatus())) {
            return attachment;
        }
        if (!"PENDING_UPLOAD".equals(attachment.uploadStatus())) {
            throw new ResidentAttachmentUploadConflictException();
        }
        ResidentAttachmentStorage.StoredObject storedObject = residentAttachmentStorage.inspect(attachment.storageKey());
        if (storedObject.byteSize() != attachment.byteSize() || !attachment.contentType().equalsIgnoreCase(storedObject.contentType())) {
            throw new ResidentAttachmentUploadConflictException();
        }
        if (!residentProfileRepository.markAttachmentUploaded(resident.tenantId(), resident.id(), attachment.id())) {
            throw new ResidentAttachmentUploadConflictException();
        }
        ResidentAttachment result = residentProfileRepository.findAttachmentById(resident.tenantId(), resident.id(), attachment.id())
                .orElseThrow(() -> new ResidentNotFoundException(residentId));
        audit("RESIDENT_ATTACHMENT_UPLOADED", "RESIDENT_ATTACHMENT", result.id(), resident.id());
        return result;
    }

    private ResidentAttachment findAttachment(Resident resident, Long attachmentId) {
        return residentProfileRepository.findAttachmentById(resident.tenantId(), resident.id(), attachmentId)
                .orElseThrow(() -> new ResidentNotFoundException(resident.id()));
    }

    public List<Resident> list(String keyword) {
        return residentRepository.findByTenantId(TenantContext.requireCurrent().tenantId(), keyword);
    }
}
