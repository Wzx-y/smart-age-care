package care.cloud.care.admission;

import care.cloud.care.resident.ResidentRepository;
import care.cloud.care.security.TenantContext;
import care.cloud.care.notification.NotificationCategory;
import care.cloud.care.notification.NotificationPriority;
import care.cloud.care.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionCreationService {
    private final AdmissionRepository admissionRepository;
    private final ResidentRepository residentRepository;
    @Autowired(required = false)
    private NotificationService notificationService;

    public AdmissionCreationService(AdmissionRepository admissionRepository, ResidentRepository residentRepository) {
        this.admissionRepository = admissionRepository;
        this.residentRepository = residentRepository;
    }

    @Transactional
    public Admission create(CreateAdmissionRequest request) {
        var principal = TenantContext.requireCurrent();
        residentRepository.findByIdAndTenantId(request.residentId(), principal.tenantId())
                .orElseThrow(() -> new AdmissionNotFoundException(request.residentId()));
        Admission admission = admissionRepository.create(principal.tenantId(), request.residentId(), principal.userId());
        if (notificationService != null) notificationService.publishBroadcast(principal.tenantId(), NotificationCategory.ADMISSION,
                NotificationPriority.NORMAL, "新的入住申请已创建", "ADMISSION", admission.id(), admission.residentId(), "ADMISSION_CREATED:" + admission.id());
        return admission;
    }
}
