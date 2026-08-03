package care.cloud.care.admission;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedAllocationPolicy;
import care.cloud.care.bed.BedRepository;
import care.cloud.care.security.TenantContext;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BedAssignmentService {
    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final long reservationTimeoutMinutes;

    public BedAssignmentService(AdmissionRepository admissionRepository, BedRepository bedRepository,
                                @org.springframework.beans.factory.annotation.Value("${admission.reservation.timeout-minutes:30}") long reservationTimeoutMinutes) {
        this.admissionRepository = admissionRepository;
        this.bedRepository = bedRepository;
        this.reservationTimeoutMinutes = reservationTimeoutMinutes;
    }

    @Transactional(rollbackFor = AdmissionVersionConflictException.class)
    public Admission assign(Long admissionId, AssignBedRequest request) {
        var principal = TenantContext.requireCurrent();
        Admission admission = admissionRepository.findByIdAndTenantId(admissionId, principal.tenantId())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));
        Bed bed = bedRepository.findByIdAndTenantId(request.bedId(), principal.tenantId())
                .orElseThrow(() -> new BedNotFoundException(request.bedId()));

        if (admission.version() != request.admissionVersion() || bed.version() != request.bedVersion()) {
            throw new AdmissionVersionConflictException();
        }
        BedAllocationPolicy.requireAssignable(admission, bed);
        OffsetDateTime reservedUntil = OffsetDateTime.now(java.time.Clock.systemUTC()).plusMinutes(reservationTimeoutMinutes);
        if (!bedRepository.reserve(bed) || !admissionRepository.assignBed(admission, bed.id(), reservedUntil, principal.userId())) {
            throw new AdmissionVersionConflictException();
        }

        return new Admission(
                admission.id(), admission.tenantId(), admission.residentId(), bed.id(), reservedUntil,
                AdmissionStatus.PENDING_CONFIRMATION, admission.version() + 1, admission.appliedAt()
        );
    }
}
