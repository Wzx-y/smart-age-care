package care.cloud.care.admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedHygieneStatus;
import care.cloud.care.bed.BedOccupancyStatus;
import care.cloud.care.bed.BedRepository;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class AdmissionReservationExpiryServiceTest {
    private final AdmissionRepository admissions = org.mockito.Mockito.mock(AdmissionRepository.class);
    private final BedRepository beds = org.mockito.Mockito.mock(BedRepository.class);
    private final AdmissionAuditRepository audit = org.mockito.Mockito.mock(AdmissionAuditRepository.class);
    private final AdmissionReservationExpiryService service = new AdmissionReservationExpiryService(admissions, beds, audit);

    @Test
    void releasesExpiredReservationAndReturnsAdmissionToPendingAssignment() throws NoSuchMethodException {
        Admission admission = expiredAdmission();
        Bed bed = new Bed(7L, 100L, 2L, "A-205-02", BedOccupancyStatus.RESERVED, BedHygieneStatus.READY, 6L);
        when(admissions.findExpiredReservations(any())).thenReturn(List.of(admission));
        when(beds.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));
        when(beds.releaseReservation(bed)).thenReturn(true);
        when(admissions.releaseExpiredReservation(admission, 0L)).thenReturn(true);

        int released = service.releaseExpiredReservations();

        assertEquals(1, released);
        Method method = AdmissionReservationExpiryService.class.getMethod("releaseExpiredReservations");
        assertNotNull(method.getAnnotation(Transactional.class));
        verify(beds).releaseReservation(bed);
        verify(admissions).releaseExpiredReservation(admission, 0L);
        verify(audit).recordReservationExpired(100L, 3L, 20L, 7L);
    }

    @Test
    void doesNotResetAdmissionWhenReservedBedCannotBeReleased() {
        Admission admission = expiredAdmission();
        Bed bed = new Bed(7L, 100L, 2L, "A-205-02", BedOccupancyStatus.OCCUPIED, BedHygieneStatus.READY, 6L);
        when(admissions.findExpiredReservations(any())).thenReturn(List.of(admission));
        when(beds.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(bed));
        when(beds.releaseReservation(bed)).thenReturn(false);

        assertEquals(0, service.releaseExpiredReservations());

        verify(admissions, never()).releaseExpiredReservation(any(), any());
        verify(audit, never()).recordReservationExpired(any(), any(), any(), any());
    }

    private Admission expiredAdmission() {
        return new Admission(3L, 100L, 20L, 7L, OffsetDateTime.now().minusMinutes(1),
                AdmissionStatus.PENDING_CONFIRMATION, 4L, OffsetDateTime.parse("2026-07-31T08:00:00Z"));
    }
}
