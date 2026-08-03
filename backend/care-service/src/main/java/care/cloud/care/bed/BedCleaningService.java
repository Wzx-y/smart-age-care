package care.cloud.care.bed;

import care.cloud.care.admission.AdmissionVersionConflictException;
import care.cloud.care.admission.AdmissionAuditRepository;
import care.cloud.care.admission.BedNotFoundException;
import care.cloud.care.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BedCleaningService {
    private final BedRepository bedRepository;
    private final AdmissionAuditRepository audit;

    public BedCleaningService(BedRepository bedRepository, AdmissionAuditRepository audit) {
        this.bedRepository = bedRepository;
        this.audit = audit;
    }

    @Transactional
    public Bed complete(Long bedId, CompleteBedCleaningRequest request) {
        var principal = TenantContext.requireCurrent();
        Bed bed = bedRepository.findByIdAndTenantId(bedId, principal.tenantId())
                .orElseThrow(() -> new BedNotFoundException(bedId));
        if (bed.version() != request.bedVersion() || bed.occupancyStatus() != BedOccupancyStatus.CLEANING
                || !bedRepository.completeCleaning(bed)) {
            throw new AdmissionVersionConflictException();
        }
        // The result is retained separately from the minimal audit event.
        bedRepository.recordCleaningResult(bed, request.result(), principal.userId());
        audit.recordCleaningCompleted(principal.tenantId(), principal.userId(), bed.id());
        return new Bed(bed.id(), bed.tenantId(), bed.roomId(), bed.bedNo(), BedOccupancyStatus.AVAILABLE,
                BedHygieneStatus.READY, bed.version() + 1);
    }

}
