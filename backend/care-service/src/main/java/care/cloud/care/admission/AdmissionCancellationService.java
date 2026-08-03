package care.cloud.care.admission;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedRepository;
import care.cloud.care.security.TenantContext;
import care.cloud.care.notification.NotificationCategory;
import care.cloud.care.notification.NotificationPriority;
import care.cloud.care.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionCancellationService {
    private final AdmissionRepository admissions;
    private final BedRepository beds;
    private final AdmissionAuditRepository audit;
    @Autowired(required = false)
    private NotificationService notificationService;

    public AdmissionCancellationService(AdmissionRepository admissions, BedRepository beds, AdmissionAuditRepository audit) {
        this.admissions = admissions;
        this.beds = beds;
        this.audit = audit;
    }

    @Transactional(rollbackFor = AdmissionVersionConflictException.class)
    public Admission cancel(Long admissionId, CancelAdmissionRequest request) {
        var principal = TenantContext.requireCurrent();
        Admission admission = admissions.findByIdAndTenantId(admissionId, principal.tenantId())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));
        if (admission.version() != request.admissionVersion() || admission.status() == AdmissionStatus.ADMITTED || admission.status() == AdmissionStatus.DISCHARGED || admission.status() == AdmissionStatus.CANCELLED) {
            throw new AdmissionVersionConflictException();
        }
        Long bedId = admission.bedId();
        if (admission.status() == AdmissionStatus.PENDING_CONFIRMATION) {
            if (bedId == null || request.bedVersion() == null) throw new AdmissionVersionConflictException();
            Bed bed = beds.findByIdAndTenantId(bedId, principal.tenantId()).orElseThrow(() -> new BedNotFoundException(bedId));
            if (bed.version() != request.bedVersion() || !beds.releaseReservation(bed)) throw new AdmissionVersionConflictException();
        }
        if (!admissions.cancel(admission, principal.userId())) throw new AdmissionVersionConflictException();
        audit.recordCancelled(principal.tenantId(), principal.userId(), admission.id(), admission.residentId(), bedId);
        if (notificationService != null) notificationService.publishBroadcast(principal.tenantId(), NotificationCategory.ADMISSION,
                NotificationPriority.NORMAL, "入住申请已取消", "ADMISSION", admission.id(), admission.residentId(), "ADMISSION_CANCELLED:" + admission.id());
        return new Admission(admission.id(), admission.tenantId(), admission.residentId(), bedId, null, AdmissionStatus.CANCELLED, admission.version() + 1, admission.appliedAt());
    }
}
