package care.cloud.care.admission;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedAllocationPolicy;
import care.cloud.care.bed.BedRepository;
import care.cloud.care.resident.ResidentAdmissionRepository;
import care.cloud.care.security.TenantContext;
import care.cloud.care.notification.NotificationCategory;
import care.cloud.care.notification.NotificationPriority;
import care.cloud.care.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionConfirmationService {
    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final ResidentAdmissionRepository residentAdmissionRepository;
    private final AdmissionAuditRepository admissionAuditRepository;
    private final NotificationService notificationService;

    public AdmissionConfirmationService(
            AdmissionRepository admissionRepository,
            BedRepository bedRepository,
            ResidentAdmissionRepository residentAdmissionRepository,
            AdmissionAuditRepository admissionAuditRepository,
            NotificationService notificationService
    ) {
        this.admissionRepository = admissionRepository;
        this.bedRepository = bedRepository;
        this.residentAdmissionRepository = residentAdmissionRepository;
        this.admissionAuditRepository = admissionAuditRepository;
        this.notificationService = notificationService;
    }

    @Transactional(rollbackFor = AdmissionVersionConflictException.class)
    public Admission confirm(Long admissionId, ConfirmAdmissionRequest request) {
        var principal = TenantContext.requireCurrent();
        Admission admission = admissionRepository.findByIdAndTenantId(admissionId, principal.tenantId())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));
        if (admission.bedId() == null || admission.reservedUntil() == null
                || !admission.reservedUntil().isAfter(java.time.OffsetDateTime.now(java.time.Clock.systemUTC()))) {
            throw new AdmissionVersionConflictException();
        }
        Bed bed = bedRepository.findByIdAndTenantId(admission.bedId(), principal.tenantId())
                .orElseThrow(() -> new BedNotFoundException(admission.bedId()));

        if (admission.version() != request.admissionVersion() || bed.version() != request.bedVersion()) {
            throw new AdmissionVersionConflictException();
        }
        BedAllocationPolicy.requireConfirmable(admission, bed);
        if (!bedRepository.occupy(bed)
                || !residentAdmissionRepository.markAdmitted(admission.residentId(), principal.tenantId(), bed.id(), principal.userId())
                || !admissionRepository.confirm(admission, principal.userId())) {
            throw new AdmissionVersionConflictException();
        }
        admissionAuditRepository.recordConfirmed(
                principal.tenantId(), principal.userId(), admission.id(), admission.residentId(), bed.id()
        );
        notificationService.publishBroadcast(principal.tenantId(), NotificationCategory.ADMISSION, NotificationPriority.NORMAL,
                "长者入住已确认", "ADMISSION", admission.id(), admission.residentId(), "ADMISSION_CONFIRMED:" + admission.id());

        return new Admission(
                admission.id(), admission.tenantId(), admission.residentId(), bed.id(), null,
                AdmissionStatus.ADMITTED, admission.version() + 1, admission.appliedAt()
        );
    }
}
