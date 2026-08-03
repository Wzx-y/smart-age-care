package care.cloud.care.admission;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedOccupancyStatus;
import care.cloud.care.bed.BedRepository;
import care.cloud.care.resident.ResidentAdmissionRepository;
import care.cloud.care.security.TenantContext;
import care.cloud.care.notification.NotificationCategory;
import care.cloud.care.notification.NotificationPriority;
import care.cloud.care.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionDischargeService {
    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final ResidentAdmissionRepository residentAdmissionRepository;
    private final AdmissionAuditRepository admissionAuditRepository;
    @Autowired(required = false)
    private NotificationService notificationService;

    public AdmissionDischargeService(
            AdmissionRepository admissionRepository,
            BedRepository bedRepository,
            ResidentAdmissionRepository residentAdmissionRepository,
            AdmissionAuditRepository admissionAuditRepository
    ) {
        this.admissionRepository = admissionRepository;
        this.bedRepository = bedRepository;
        this.residentAdmissionRepository = residentAdmissionRepository;
        this.admissionAuditRepository = admissionAuditRepository;
    }

    @Transactional(rollbackFor = AdmissionVersionConflictException.class)
    public Admission discharge(Long admissionId, DischargeAdmissionRequest request) {
        var principal = TenantContext.requireCurrent();
        Admission admission = admissionRepository.findByIdAndTenantId(admissionId, principal.tenantId())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));
        if (admission.status() != AdmissionStatus.ADMITTED || admission.bedId() == null) {
            throw new AdmissionVersionConflictException();
        }
        Bed bed = bedRepository.findByIdAndTenantId(admission.bedId(), principal.tenantId())
                .orElseThrow(() -> new BedNotFoundException(admission.bedId()));
        if (admission.version() != request.admissionVersion() || bed.version() != request.bedVersion()
                || bed.occupancyStatus() != BedOccupancyStatus.OCCUPIED) {
            throw new AdmissionVersionConflictException();
        }
        if (!bedRepository.releaseForCleaning(bed)
                || !residentAdmissionRepository.markDischarged(admission.residentId(), principal.tenantId(), bed.id(), principal.userId())
                || !admissionRepository.discharge(admission, principal.userId(), request.dischargeReason())) {
            throw new AdmissionVersionConflictException();
        }
        admissionAuditRepository.recordDischarged(
                principal.tenantId(), principal.userId(), admission.id(), admission.residentId(), bed.id()
        );
        if (notificationService != null) notificationService.publishBroadcast(principal.tenantId(), NotificationCategory.ADMISSION,
                NotificationPriority.NORMAL, "长者已办理退住", "ADMISSION", admission.id(), admission.residentId(), "ADMISSION_DISCHARGED:" + admission.id());
        return new Admission(
                admission.id(), admission.tenantId(), admission.residentId(), bed.id(), null,
                AdmissionStatus.DISCHARGED, admission.version() + 1, admission.appliedAt()
        );
    }
}
