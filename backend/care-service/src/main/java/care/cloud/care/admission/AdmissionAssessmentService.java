package care.cloud.care.admission;

import care.cloud.care.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionAssessmentService {
    private final AdmissionRepository admissions;
    private final AdmissionAuditRepository audit;

    public AdmissionAssessmentService(AdmissionRepository admissions, AdmissionAuditRepository audit) {
        this.admissions = admissions;
        this.audit = audit;
    }

    @Transactional(rollbackFor = AdmissionVersionConflictException.class)
    public Admission decide(Long admissionId, DecideAdmissionAssessmentRequest request) {
        var principal = TenantContext.requireCurrent();
        Admission admission = admissions.findByIdAndTenantId(admissionId, principal.tenantId())
                .orElseThrow(() -> new AdmissionNotFoundException(admissionId));
        if (admission.version() != request.admissionVersion()
                || (admission.status() != AdmissionStatus.PENDING_ASSESSMENT
                && admission.status() != AdmissionStatus.ASSESSMENT_REJECTED)) {
            throw new AdmissionVersionConflictException();
        }
        AdmissionStatus nextStatus = request.decision() == AdmissionAssessmentDecisionStatus.PASSED
                ? AdmissionStatus.PENDING_ASSIGNMENT : AdmissionStatus.ASSESSMENT_REJECTED;
        if (!admissions.decideAssessment(admission, nextStatus, request, principal.userId())) {
            throw new AdmissionVersionConflictException();
        }
        audit.recordAssessmentDecided(principal.tenantId(), principal.userId(), admission.id(), admission.residentId(), request.decision());
        return new Admission(admission.id(), admission.tenantId(), admission.residentId(), admission.bedId(), admission.reservedUntil(),
                nextStatus, admission.version() + 1, admission.appliedAt());
    }
}
