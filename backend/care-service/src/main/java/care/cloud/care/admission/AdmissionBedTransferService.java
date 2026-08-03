package care.cloud.care.admission;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedAllocationPolicy;
import care.cloud.care.bed.BedRepository;
import care.cloud.care.resident.ResidentAdmissionRepository;
import care.cloud.care.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionBedTransferService {
    private final AdmissionRepository admissions;
    private final BedRepository beds;
    private final ResidentAdmissionRepository residents;
    private final AdmissionAuditRepository audit;

    public AdmissionBedTransferService(AdmissionRepository admissions, BedRepository beds,
                                       ResidentAdmissionRepository residents, AdmissionAuditRepository audit) {
        this.admissions = admissions;
        this.beds = beds;
        this.residents = residents;
        this.audit = audit;
    }

    @Transactional(rollbackFor = AdmissionVersionConflictException.class)
    public Admission transfer(Long admissionId, TransferBedRequest request) {
        var principal = TenantContext.requireCurrent();
        Admission admission = admissions.findByIdAndTenantId(admissionId, principal.tenantId())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));
        if (admission.status() != AdmissionStatus.ADMITTED || admission.bedId() == null
                || admission.version() != request.admissionVersion() || admission.bedId().equals(request.targetBedId())) {
            throw new AdmissionVersionConflictException();
        }
        Bed source = beds.findByIdAndTenantId(admission.bedId(), principal.tenantId())
                .orElseThrow(() -> new BedNotFoundException(admission.bedId()));
        Bed target = beds.findByIdAndTenantId(request.targetBedId(), principal.tenantId())
                .orElseThrow(() -> new BedNotFoundException(request.targetBedId()));
        if (source.version() != request.sourceBedVersion() || target.version() != request.targetBedVersion()) {
            throw new AdmissionVersionConflictException();
        }
        BedAllocationPolicy.requireTransferable(admission, source, target);
        if (!beds.occupyAvailable(target) || !beds.releaseForCleaning(source)
                || !residents.moveBed(admission.residentId(), principal.tenantId(), source.id(), target.id(), principal.userId())
                || !admissions.transferBed(admission, request, principal.userId())) {
            throw new AdmissionVersionConflictException();
        }
        audit.recordBedTransferred(principal.tenantId(), principal.userId(), admission.id(), admission.residentId(), source.id(), target.id());
        return new Admission(admission.id(), admission.tenantId(), admission.residentId(), target.id(), null,
                AdmissionStatus.ADMITTED, admission.version() + 1, admission.appliedAt());
    }
}
