package care.cloud.care.admission;

import care.cloud.care.bed.Bed;
import care.cloud.care.bed.BedRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import care.cloud.care.notification.NotificationCategory;
import care.cloud.care.notification.NotificationPriority;
import care.cloud.care.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AdmissionReservationExpiryService {
    private final AdmissionRepository admissions;
    private final BedRepository beds;
    private final AdmissionAuditRepository audit;
    @Autowired(required = false)
    private NotificationService notificationService;

    public AdmissionReservationExpiryService(AdmissionRepository admissions, BedRepository beds, AdmissionAuditRepository audit) {
        this.admissions = admissions;
        this.beds = beds;
        this.audit = audit;
    }

    @Transactional(rollbackFor = AdmissionVersionConflictException.class)
    public int releaseExpiredReservations() {
        int released = 0;
        for (Admission admission : admissions.findExpiredReservations(OffsetDateTime.now(java.time.Clock.systemUTC()))) {
            if (admission.bedId() == null) continue;
            Bed bed = beds.findByIdAndTenantId(admission.bedId(), admission.tenantId()).orElse(null);
            if (bed == null || !beds.releaseReservation(bed)) continue;
            if (!admissions.releaseExpiredReservation(admission, 0L)) throw new AdmissionVersionConflictException();
            audit.recordReservationExpired(admission.tenantId(), admission.id(), admission.residentId(), admission.bedId());
            if (notificationService != null) notificationService.publishBroadcast(admission.tenantId(), NotificationCategory.ADMISSION,
                    NotificationPriority.NORMAL, "入住床位预留已超时释放", "ADMISSION", admission.id(), admission.residentId(), "ADMISSION_RESERVATION_EXPIRED:" + admission.id());
            released++;
        }
        return released;
    }
}
