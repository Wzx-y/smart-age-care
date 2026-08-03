package care.cloud.care.admission;

import care.cloud.care.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdmissionQueryService {
    private final AdmissionRepository admissionRepository;

    public AdmissionQueryService(AdmissionRepository admissionRepository) {
        this.admissionRepository = admissionRepository;
    }

    public List<AdmissionSummary> list() {
        return admissionRepository.findByTenantId(TenantContext.requireCurrent().tenantId());
    }

    public List<AdmissionAuditEvent> audit(Long admissionId) {
        var principal = TenantContext.requireCurrent();
        admissionRepository.findByIdAndTenantId(admissionId, principal.tenantId())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));
        return admissionRepository.findAuditEvents(principal.tenantId(), admissionId);
    }
}
