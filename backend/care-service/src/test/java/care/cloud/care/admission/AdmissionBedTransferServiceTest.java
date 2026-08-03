package care.cloud.care.admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedHygieneStatus;
import care.cloud.care.bed.BedOccupancyStatus;
import care.cloud.care.bed.BedRepository;
import care.cloud.care.resident.ResidentAdmissionRepository;
import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdmissionBedTransferServiceTest {
    private final AdmissionRepository admissions = org.mockito.Mockito.mock(AdmissionRepository.class);
    private final BedRepository beds = org.mockito.Mockito.mock(BedRepository.class);
    private final ResidentAdmissionRepository residents = org.mockito.Mockito.mock(ResidentAdmissionRepository.class);
    private final AdmissionAuditRepository audit = org.mockito.Mockito.mock(AdmissionAuditRepository.class);
    private final AdmissionBedTransferService service = new AdmissionBedTransferService(admissions, beds, residents, audit);

    @BeforeEach void setContext() { TenantContext.set(new TenantPrincipal(801L, 100L)); }
    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test
    void transfersResidentAndSendsSourceBedToCleaningInOneTransaction() {
        Admission admission = new Admission(3L, 100L, 20L, 7L, null, AdmissionStatus.ADMITTED, 4L, OffsetDateTime.now());
        Bed source = new Bed(7L, 100L, 2L, "A-205-01", BedOccupancyStatus.OCCUPIED, BedHygieneStatus.READY, 6L);
        Bed target = new Bed(8L, 100L, 2L, "A-205-02", BedOccupancyStatus.AVAILABLE, BedHygieneStatus.READY, 2L);
        TransferBedRequest request = new TransferBedRequest(8L, 4L, 6L, 2L, "夜间照护距离调整");
        when(admissions.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(beds.findByIdAndTenantId(7L, 100L)).thenReturn(Optional.of(source));
        when(beds.findByIdAndTenantId(8L, 100L)).thenReturn(Optional.of(target));
        when(beds.occupyAvailable(target)).thenReturn(true);
        when(beds.releaseForCleaning(source)).thenReturn(true);
        when(residents.moveBed(20L, 100L, 7L, 8L, 801L)).thenReturn(true);
        when(admissions.transferBed(admission, request, 801L)).thenReturn(true);

        Admission result = service.transfer(3L, request);

        assertEquals(8L, result.bedId());
        verify(audit).recordBedTransferred(100L, 801L, 3L, 20L, 7L, 8L);
    }
}
