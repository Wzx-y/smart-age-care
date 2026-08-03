package care.cloud.care.admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedHygieneStatus;
import care.cloud.care.bed.BedOccupancyStatus;
import care.cloud.care.bed.BedRepository;
import care.cloud.care.resident.ResidentAdmissionRepository;
import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class AdmissionDischargeServiceTest {
    private final AdmissionRepository admissionRepository = org.mockito.Mockito.mock(AdmissionRepository.class);
    private final BedRepository bedRepository = org.mockito.Mockito.mock(BedRepository.class);
    private final ResidentAdmissionRepository residentAdmissionRepository = org.mockito.Mockito.mock(ResidentAdmissionRepository.class);
    private final AdmissionAuditRepository admissionAuditRepository = org.mockito.Mockito.mock(AdmissionAuditRepository.class);
    private final AdmissionDischargeService service = new AdmissionDischargeService(
            admissionRepository, bedRepository, residentAdmissionRepository, admissionAuditRepository
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
    void dischargesAdmissionReleasesResidentAndMovesBedToCleaning() {
        Admission admission = admittedAdmission(4L);
        Bed bed = occupiedBed(7L, 6L);
        DischargeAdmissionRequest request = new DischargeAdmissionRequest(4L, 6L, "转至家属照护");
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));
        when(bedRepository.releaseForCleaning(bed)).thenReturn(true);
        when(residentAdmissionRepository.markDischarged(20L, 100L, 7L, 801L)).thenReturn(true);
        when(admissionRepository.discharge(admission, 801L, request.dischargeReason())).thenReturn(true);

        Admission discharged = service.discharge(3L, request);

        assertEquals(AdmissionStatus.DISCHARGED, discharged.status());
        assertEquals(5L, discharged.version());
        verify(bedRepository).releaseForCleaning(bed);
        verify(residentAdmissionRepository).markDischarged(20L, 100L, 7L, 801L);
        verify(admissionRepository).discharge(admission, 801L, request.dischargeReason());
        verify(admissionAuditRepository).recordDischarged(100L, 801L, 3L, 20L, 7L);
    }

    @Test
    void rejectsDischargeWhenTheCurrentBedIsNotOccupied() {
        Admission admission = admittedAdmission(4L);
        Bed bed = new Bed(7L, 100L, 2L, "A-205-02", BedOccupancyStatus.CLEANING, BedHygieneStatus.READY, 6L);
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));

        assertThrows(AdmissionVersionConflictException.class, () -> service.discharge(3L, new DischargeAdmissionRequest(4L, 6L, "转至家属照护")));

        verify(bedRepository, never()).releaseForCleaning(org.mockito.ArgumentMatchers.any());
        verify(residentAdmissionRepository, never()).markDischarged(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(admissionRepository, never()).discharge(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(admissionAuditRepository, never()).recordDischarged(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void signalsRollbackWhenAdmissionDischargeLosesOptimisticLock() throws NoSuchMethodException {
        Admission admission = admittedAdmission(4L);
        Bed bed = occupiedBed(7L, 6L);
        DischargeAdmissionRequest request = new DischargeAdmissionRequest(4L, 6L, "转至家属照护");
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));
        when(bedRepository.releaseForCleaning(bed)).thenReturn(true);
        when(residentAdmissionRepository.markDischarged(20L, 100L, 7L, 801L)).thenReturn(true);
        when(admissionRepository.discharge(admission, 801L, request.dischargeReason())).thenReturn(false);

        Method discharge = AdmissionDischargeService.class.getMethod("discharge", Long.class, DischargeAdmissionRequest.class);
        assertNotNull(discharge.getAnnotation(Transactional.class));
        assertThrows(AdmissionVersionConflictException.class, () -> service.discharge(3L, request));

        verify(bedRepository).releaseForCleaning(bed);
        verify(residentAdmissionRepository).markDischarged(20L, 100L, 7L, 801L);
        verify(admissionRepository).discharge(admission, 801L, request.dischargeReason());
        verify(admissionAuditRepository, never()).recordDischarged(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private Admission admittedAdmission(long version) {
        return new Admission(3L, 100L, 20L, 7L, null, AdmissionStatus.ADMITTED, version, OffsetDateTime.parse("2026-07-31T08:00:00Z"));
    }

    private Bed occupiedBed(long id, long version) {
        return new Bed(id, 100L, 2L, "A-205-02", BedOccupancyStatus.OCCUPIED, BedHygieneStatus.READY, version);
    }
}
