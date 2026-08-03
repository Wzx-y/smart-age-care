package care.cloud.care.resident;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResidentServiceTest {
    private final ResidentRepository residentRepository = org.mockito.Mockito.mock(ResidentRepository.class);
    private final ResidentProfileRepository residentProfileRepository = org.mockito.Mockito.mock(ResidentProfileRepository.class);
    private final ResidentAttachmentStorage residentAttachmentStorage = org.mockito.Mockito.mock(ResidentAttachmentStorage.class);
    private final ResidentService residentService = new ResidentService(residentRepository, residentProfileRepository, residentAttachmentStorage);

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(new TenantPrincipal(801L, 100L));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void updatesOnlyResidentProfileFieldsInTheCurrentTenant() {
        Resident current = resident(ResidentStatus.PENDING_ADMISSION);
        Resident persisted = new Resident(11L, 100L, "李秀兰", "女", LocalDate.of(1942, 5, 16),
                ResidentStatus.PENDING_ADMISSION, null, "李海", "13800138001", current.createdAt(), OffsetDateTime.parse("2026-07-31T08:01:00Z"));
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(current), Optional.of(persisted));
        when(residentRepository.update(org.mockito.ArgumentMatchers.any(Resident.class))).thenReturn(true);

        Resident updated = residentService.update(11L, new UpdateResidentRequest(
                "李秀兰", "女", LocalDate.of(1942, 5, 16), "李海", "13800138001"));

        assertEquals("李秀兰", updated.name());
        assertEquals(ResidentStatus.PENDING_ADMISSION, updated.status());
        verify(residentRepository).update(org.mockito.ArgumentMatchers.argThat(record ->
                record.id().equals(11L)
                        && record.tenantId().equals(100L)
                        && record.status() == ResidentStatus.PENDING_ADMISSION
                        && record.currentBedId() == null));
    }

    @Test
    void archivesOnlyResidentsWhoseCareHasAlreadyClosed() {
        Resident discharged = resident(ResidentStatus.DISCHARGED);
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(discharged));
        when(residentRepository.archive(org.mockito.ArgumentMatchers.eq(11L), org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.any())).thenReturn(true);

        residentService.archive(11L);

        verify(residentRepository).archive(org.mockito.ArgumentMatchers.eq(11L), org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsArchivingAnInResidenceResident() {
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident(ResidentStatus.IN_RESIDENCE)));

        assertThrows(ResidentArchiveConflictException.class, () -> residentService.archive(11L));

        verify(residentRepository, never()).archive(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void replacesThePrimaryContactInsideOneProfileWrite() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        CreateResidentContactRequest request = new CreateResidentContactRequest("王敏", "女儿", "13800138001", true);
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.saveContact(100L, 11L, request))
                .thenReturn(new ResidentContact(21L, 11L, "王敏", "女儿", "13800138001", true, 1L));

        ResidentContact contact = residentService.createContact(11L, request);

        assertEquals("王敏", contact.name());
        verify(residentProfileRepository).clearPrimaryContact(100L, 11L);
        verify(residentProfileRepository).saveContact(100L, 11L, request);
    }

    @Test
    void recordsTheCurrentMemberAsTheAssessmentAuthor() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        CreateResidentAssessmentRequest request = new CreateResidentAssessmentRequest(
                "ADL", LocalDate.of(2026, 7, 31), 60, "MEDIUM", "需要协助洗浴");
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.saveAssessment(100L, 11L, 801L, request))
                .thenReturn(new ResidentAssessment(31L, 11L, "ADL", request.assessmentDate(), 60, "MEDIUM", request.note(), 801L, 1L, OffsetDateTime.now()));

        ResidentAssessment assessment = residentService.createAssessment(11L, request);

        assertEquals(801L, assessment.assessorId());
        verify(residentProfileRepository).saveAssessment(100L, 11L, 801L, request);
    }

    @Test
    void updatesPrimaryContactWithItsCurrentVersion() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        UpdateResidentContactRequest request = new UpdateResidentContactRequest(2L, "王敏", "女儿", "13800138001", true);
        ResidentContact updated = new ResidentContact(21L, 11L, "王敏", "女儿", "13800138001", true, 3L);
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.updateContact(100L, 11L, 21L, request)).thenReturn(true);
        when(residentProfileRepository.findContacts(100L, 11L)).thenReturn(java.util.List.of(updated));

        assertEquals(updated, residentService.updateContact(11L, 21L, request));

        verify(residentProfileRepository).clearPrimaryContactExcept(100L, 11L, 21L);
        verify(residentProfileRepository).updateContact(100L, 11L, 21L, request);
    }

    @Test
    void preventsDeletingAnAssessmentReferencedByAnActiveCarePlan() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.isAssessmentReferenced(100L, 11L, 31L)).thenReturn(true);

        assertThrows(ResidentProfileConflictException.class, () -> residentService.deleteAssessment(11L, 31L, 2L));

        verify(residentProfileRepository, never()).deleteAssessment(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void createsAnOpaqueAttachmentStorageKey() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        CreateResidentAttachmentRequest request = new CreateResidentAttachmentRequest("王秀兰病历.pdf", "application/pdf", 1024L);
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.saveAttachment(org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.eq(11L), org.mockito.ArgumentMatchers.eq(801L), org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> attachment((String) invocation.getArgument(4), "PENDING_UPLOAD", null));

        ResidentAttachment attachment = residentService.prepareAttachment(11L, request);

        assertEquals(false, attachment.storageKey().contains(request.fileName()));
        verify(residentProfileRepository).saveAttachment(org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.eq(11L), org.mockito.ArgumentMatchers.eq(801L), org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void signsOnlyPendingAttachmentsWithinTheCurrentResidentScope() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        ResidentAttachment attachment = attachment("resident-attachments/100/11/opaque-id", "PENDING_UPLOAD", null);
        ResidentAttachmentUploadTarget expectedTarget = new ResidentAttachmentUploadTarget(
                "https://storage.example/upload-token", OffsetDateTime.parse("2026-07-31T08:10:00Z"));
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.findAttachmentById(100L, 11L, 41L)).thenReturn(Optional.of(attachment));
        when(residentAttachmentStorage.signUpload(attachment.storageKey(), attachment.contentType())).thenReturn(expectedTarget);

        ResidentAttachmentUploadTarget target = residentService.createAttachmentUploadTarget(11L, 41L);

        assertEquals(expectedTarget, target);
        verify(residentAttachmentStorage).signUpload(attachment.storageKey(), attachment.contentType());
    }

    @Test
    void rejectsSigningAttachmentsOutsideTheCurrentTenantResidentScope() {
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.empty());

        assertThrows(ResidentNotFoundException.class, () -> residentService.createAttachmentUploadTarget(11L, 41L));

        verify(residentProfileRepository, never()).findAttachmentById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(residentAttachmentStorage, never()).signUpload(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void completesUploadOnlyAfterStorageMetadataMatchesTheAttachment() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        ResidentAttachment pending = attachment("resident-attachments/100/11/opaque-id", "PENDING_UPLOAD", null);
        ResidentAttachment uploaded = attachment("resident-attachments/100/11/opaque-id", "UPLOADED", OffsetDateTime.parse("2026-07-31T08:05:00Z"));
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.findAttachmentById(100L, 11L, 41L)).thenReturn(Optional.of(pending), Optional.of(uploaded));
        when(residentAttachmentStorage.inspect(pending.storageKey()))
                .thenReturn(new ResidentAttachmentStorage.StoredObject(pending.contentType(), pending.byteSize()));
        when(residentProfileRepository.markAttachmentUploaded(100L, 11L, 41L)).thenReturn(true);

        ResidentAttachment result = residentService.completeAttachmentUpload(11L, 41L);

        assertEquals("UPLOADED", result.uploadStatus());
        verify(residentProfileRepository).markAttachmentUploaded(100L, 11L, 41L);
    }

    @Test
    void rejectsCompletionWhenStoredMetadataDoesNotMatchAndLeavesTheAttachmentPending() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        ResidentAttachment pending = attachment("resident-attachments/100/11/opaque-id", "PENDING_UPLOAD", null);
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.findAttachmentById(100L, 11L, 41L)).thenReturn(Optional.of(pending));
        when(residentAttachmentStorage.inspect(pending.storageKey()))
                .thenReturn(new ResidentAttachmentStorage.StoredObject("image/png", pending.byteSize() + 1));

        assertThrows(ResidentAttachmentUploadConflictException.class, () -> residentService.completeAttachmentUpload(11L, 41L));

        verify(residentProfileRepository, never()).markAttachmentUploaded(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsAnAlreadyCompletedUploadWithoutInspectingStorageAgain() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        ResidentAttachment uploaded = attachment("resident-attachments/100/11/opaque-id", "UPLOADED", OffsetDateTime.parse("2026-07-31T08:05:00Z"));
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.findAttachmentById(100L, 11L, 41L)).thenReturn(Optional.of(uploaded));

        ResidentAttachment result = residentService.completeAttachmentUpload(11L, 41L);

        assertEquals(uploaded, result);
        verify(residentAttachmentStorage, never()).inspect(org.mockito.ArgumentMatchers.any());
        verify(residentProfileRepository, never()).markAttachmentUploaded(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void leavesTheAttachmentPendingWhenStorageIsUnavailable() {
        Resident resident = resident(ResidentStatus.IN_RESIDENCE);
        ResidentAttachment pending = attachment("resident-attachments/100/11/opaque-id", "PENDING_UPLOAD", null);
        when(residentRepository.findByIdAndTenantId(11L, 100L)).thenReturn(Optional.of(resident));
        when(residentProfileRepository.findAttachmentById(100L, 11L, 41L)).thenReturn(Optional.of(pending));
        when(residentAttachmentStorage.inspect(pending.storageKey())).thenThrow(new ResidentAttachmentStorageUnavailableException());

        assertThrows(ResidentAttachmentStorageUnavailableException.class, () -> residentService.completeAttachmentUpload(11L, 41L));

        verify(residentProfileRepository, never()).markAttachmentUploaded(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private ResidentAttachment attachment(String storageKey, String uploadStatus, OffsetDateTime uploadedAt) {
        return new ResidentAttachment(41L, 11L, "王秀兰病历.pdf", "application/pdf", 1024L, storageKey,
                uploadStatus, OffsetDateTime.parse("2026-07-31T08:00:00Z"), uploadedAt);
    }

    private Resident resident(ResidentStatus status) {
        return new Resident(11L, 100L, "王秀兰", "女", LocalDate.of(1944, 3, 20), status,
                status == ResidentStatus.IN_RESIDENCE ? 77L : null, "王海", "13900139000",
                OffsetDateTime.parse("2026-07-31T08:00:00Z"), OffsetDateTime.parse("2026-07-31T08:00:00Z"));
    }
}
