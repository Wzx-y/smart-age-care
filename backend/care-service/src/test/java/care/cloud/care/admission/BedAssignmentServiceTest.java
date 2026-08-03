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
import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class BedAssignmentServiceTest {
    private final AdmissionRepository admissionRepository = org.mockito.Mockito.mock(AdmissionRepository.class);
    private final BedRepository bedRepository = org.mockito.Mockito.mock(BedRepository.class);
    private final BedAssignmentService service = new BedAssignmentService(admissionRepository, bedRepository, 30L);

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(new TenantPrincipal(801L, 100L));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void reservesReadyBedForAdmissionInCurrentTenant() {
        Admission admission = pendingAdmission(3L);
        Bed bed = readyBed(7L, 5L);
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));
        when(bedRepository.reserve(bed)).thenReturn(true);
        when(admissionRepository.assignBed(org.mockito.ArgumentMatchers.eq(admission), org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(801L))).thenReturn(true);

        Admission assigned = service.assign(3L, new AssignBedRequest(7L, 3L, 5L));

        assertEquals(7L, assigned.bedId());
        assertEquals(AdmissionStatus.PENDING_CONFIRMATION, assigned.status());
        assertNotNull(assigned.reservedUntil());
        assertEquals(4L, assigned.version());
        verify(bedRepository).reserve(bed);
        verify(admissionRepository).assignBed(org.mockito.ArgumentMatchers.eq(admission), org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(801L));
    }

    @Test
    void doesNotRevealBedFromAnotherTenant() {
        Admission admission = pendingAdmission(3L);
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.empty());

        assertThrows(BedNotFoundException.class, () -> service.assign(3L, new AssignBedRequest(7L, 3L, 5L)));

        verify(bedRepository, never()).reserve(org.mockito.ArgumentMatchers.any());
        verify(admissionRepository, never()).assignBed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsOccupiedOrUnpreparedBedBeforeWriting() {
        Admission admission = pendingAdmission(3L);
        Bed occupiedBed = new Bed(7L, 100L, 2L, "A-205-02", BedOccupancyStatus.OCCUPIED, BedHygieneStatus.READY, 5L);
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(occupiedBed));

        assertThrows(BedAllocationException.class, () -> service.assign(3L, new AssignBedRequest(7L, 3L, 5L)));

        verify(bedRepository, never()).reserve(org.mockito.ArgumentMatchers.any());
        verify(admissionRepository, never()).assignBed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsStaleAdmissionOrBedVersionBeforeWriting() {
        Admission admission = pendingAdmission(3L);
        Bed bed = readyBed(7L, 5L);
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));

        assertThrows(AdmissionVersionConflictException.class, () -> service.assign(3L, new AssignBedRequest(7L, 2L, 5L)));

        verify(bedRepository, never()).reserve(org.mockito.ArgumentMatchers.any());
        verify(admissionRepository, never()).assignBed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void signalsRollbackWhenAdmissionUpdateLosesOptimisticLockAfterReservation() throws NoSuchMethodException {
        Admission admission = pendingAdmission(3L);
        Bed bed = readyBed(7L, 5L);
        when(admissionRepository.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(bedRepository.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));
        when(bedRepository.reserve(bed)).thenReturn(true);
        when(admissionRepository.assignBed(org.mockito.ArgumentMatchers.eq(admission), org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(801L))).thenReturn(false);

        Method assign = BedAssignmentService.class.getMethod("assign", Long.class, AssignBedRequest.class);
        assertNotNull(assign.getAnnotation(Transactional.class));
        assertThrows(AdmissionVersionConflictException.class, () -> service.assign(3L, new AssignBedRequest(7L, 3L, 5L)));

        verify(bedRepository).reserve(bed);
        verify(admissionRepository).assignBed(org.mockito.ArgumentMatchers.eq(admission), org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(801L));
    }

    private Admission pendingAdmission(long version) {
        return new Admission(3L, 100L, 20L, null, null, AdmissionStatus.PENDING_ASSIGNMENT, version, OffsetDateTime.parse("2026-07-29T08:00:00Z"));
    }

    private Bed readyBed(long id, long version) {
        return new Bed(id, 100L, 2L, "A-205-02", BedOccupancyStatus.AVAILABLE, BedHygieneStatus.READY, version);
    }
}
