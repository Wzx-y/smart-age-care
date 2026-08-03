package care.cloud.care.admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedAllocationException;
import care.cloud.care.bed.BedHygieneStatus;
import care.cloud.care.bed.BedOccupancyStatus;
import care.cloud.care.bed.BedRepository;
import care.cloud.care.resident.ResidentAdmissionRepository;
import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import care.cloud.care.notification.NotificationService;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class AdmissionConfirmationServiceTest {
    private final AdmissionRepository admissionRepository = org.mockito.Mockito.mock(AdmissionRepository.class);
    private final BedRepository bedRepository = org.mockito.Mockito.mock(BedRepository.class);
    private final ResidentAdmissionRepository residentAdmissionRepository = org.mockito.Mockito.mock(ResidentAdmissionRepository.class);
    private final AdmissionAuditRepository admissionAuditRepository = org.mockito.Mockito.mock(AdmissionAuditRepository.class);
    private final NotificationService notificationService = org.mockito.Mockito.mock(NotificationService.class);
    private final AdmissionConfirmationService service = new AdmissionConfirmationService(
            admissionRepository, bedRepository, residentAdmissionRepository, admissionAuditRepository, notificationService
    );

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(new TenantPrincipal(801L, 100L));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void confirmsAdmissionAndUpdatesAllDomainRecords() {
        Admission admission = pendingConfirmationAdmission(4L);
        Bed bed = reservedBed(7L, 6L);
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));
        when(bedRepository.occupy(bed)).thenReturn(true);
        when(residentAdmissionRepository.markAdmitted(20L, 100L, 7L, 801L)).thenReturn(true);
        when(admissionRepository.confirm(admission, 801L)).thenReturn(true);

        Admission confirmed = service.confirm(3L, new ConfirmAdmissionRequest(4L, 6L));

        assertEquals(AdmissionStatus.ADMITTED, confirmed.status());
        assertEquals(5L, confirmed.version());
        verify(bedRepository).occupy(bed);
        verify(residentAdmissionRepository).markAdmitted(20L, 100L, 7L, 801L);
        verify(admissionRepository).confirm(admission, 801L);
        verify(admissionAuditRepository).recordConfirmed(100L, 801L, 3L, 20L, 7L);
    }

    @Test
    void rejectsConfirmationWhenBedIsNotReserved() {
        Admission admission = pendingConfirmationAdmission(4L);
        Bed bed = new Bed(7L, 100L, 2L, "A-205-02", BedOccupancyStatus.AVAILABLE, BedHygieneStatus.READY, 6L);
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));

        assertThrows(BedAllocationException.class, () -> service.confirm(3L, new ConfirmAdmissionRequest(4L, 6L)));

        verify(bedRepository, never()).occupy(org.mockito.ArgumentMatchers.any());
        verify(residentAdmissionRepository, never()).markAdmitted(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(admissionRepository, never()).confirm(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(admissionAuditRepository, never()).recordConfirmed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void signalsRollbackWhenAdmissionUpdateLosesOptimisticLockAfterOtherWrites() throws NoSuchMethodException {
        Admission admission = pendingConfirmationAdmission(4L);
        Bed bed = reservedBed(7L, 6L);
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));
        when(bedRepository.occupy(bed)).thenReturn(true);
        when(residentAdmissionRepository.markAdmitted(20L, 100L, 7L, 801L)).thenReturn(true);
        when(admissionRepository.confirm(admission, 801L)).thenReturn(false);

        Method confirm = AdmissionConfirmationService.class.getMethod("confirm", Long.class, ConfirmAdmissionRequest.class);
        assertNotNull(confirm.getAnnotation(Transactional.class));
        assertThrows(AdmissionVersionConflictException.class, () -> service.confirm(3L, new ConfirmAdmissionRequest(4L, 6L)));

        verify(bedRepository).occupy(bed);
        verify(residentAdmissionRepository).markAdmitted(20L, 100L, 7L, 801L);
        verify(admissionRepository).confirm(admission, 801L);
        verify(admissionAuditRepository, never()).recordConfirmed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private Admission pendingConfirmationAdmission(long version) {
        return new Admission(3L, 100L, 20L, 7L, OffsetDateTime.parse("2026-07-29T08:30:00Z"), AdmissionStatus.PENDING_CONFIRMATION, version, OffsetDateTime.parse("2026-07-29T08:00:00Z"));
    }

    private Bed reservedBed(long id, long version) {
        return new Bed(id, 100L, 2L, "A-205-02", BedOccupancyStatus.RESERVED, BedHygieneStatus.READY, version);
    }
}
