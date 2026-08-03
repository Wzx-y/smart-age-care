package care.cloud.care.bed;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import care.cloud.care.admission.Admission;
import care.cloud.care.admission.AdmissionStatus;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class BedAllocationPolicyTest {
    private final Admission admission = new Admission(
            1L, 100L, 200L, null, null, AdmissionStatus.PENDING_ASSIGNMENT, 0L, OffsetDateTime.now()
    );

    @Test
    void allocatesOnlyReadyAvailableBedInSameTenant() {
        Bed bed = new Bed(10L, 100L, 20L, "A-205-02", BedOccupancyStatus.AVAILABLE, BedHygieneStatus.READY, 0L);
        assertDoesNotThrow(() -> BedAllocationPolicy.requireAssignable(admission, bed));
    }

    @Test
    void rejectsBedFromAnotherTenant() {
        Bed bed = new Bed(10L, 101L, 20L, "A-205-02", BedOccupancyStatus.AVAILABLE, BedHygieneStatus.READY, 0L);
        assertThrows(BedAllocationException.class, () -> BedAllocationPolicy.requireAssignable(admission, bed));
    }

    @Test
    void rejectsOccupiedOrUnpreparedBed() {
        Bed occupied = new Bed(10L, 100L, 20L, "A-205-02", BedOccupancyStatus.OCCUPIED, BedHygieneStatus.READY, 0L);
        Bed unprepared = new Bed(11L, 100L, 20L, "A-205-03", BedOccupancyStatus.AVAILABLE, BedHygieneStatus.CLEANING, 0L);

        assertThrows(BedAllocationException.class, () -> BedAllocationPolicy.requireAssignable(admission, occupied));
        assertThrows(BedAllocationException.class, () -> BedAllocationPolicy.requireAssignable(admission, unprepared));
    }
}
