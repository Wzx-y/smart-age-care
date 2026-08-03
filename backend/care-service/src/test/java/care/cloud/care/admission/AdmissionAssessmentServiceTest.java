package care.cloud.care.admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdmissionAssessmentServiceTest {
    private final AdmissionRepository admissions = org.mockito.Mockito.mock(AdmissionRepository.class);
    private final AdmissionAuditRepository audit = org.mockito.Mockito.mock(AdmissionAuditRepository.class);
    private final AdmissionAssessmentService service = new AdmissionAssessmentService(admissions, audit);

    @BeforeEach void setContext() { TenantContext.set(new TenantPrincipal(801L, 100L)); }
    @AfterEach void clearContext() { TenantContext.clear(); }

    @Test
    void passesAssessmentBeforeBedAllocation() {
        Admission admission = pending(4L);
        DecideAdmissionAssessmentRequest request = new DecideAdmissionAssessmentRequest(4L, AdmissionAssessmentDecisionStatus.PASSED, "生活照护需求已核验");
        when(admissions.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(admission));
        when(admissions.decideAssessment(admission, AdmissionStatus.PENDING_ASSIGNMENT, request, 801L)).thenReturn(true);

        Admission result = service.decide(3L, request);

        assertEquals(AdmissionStatus.PENDING_ASSIGNMENT, result.status());
        verify(audit).recordAssessmentDecided(100L, 801L, 3L, 20L, AdmissionAssessmentDecisionStatus.PASSED);
    }

    @Test
    void rejectsStaleAssessmentDecision() {
        when(admissions.findByIdAndTenantId(3L, 100L)).thenReturn(Optional.of(pending(4L)));

        assertThrows(AdmissionVersionConflictException.class, () -> service.decide(3L,
                new DecideAdmissionAssessmentRequest(3L, AdmissionAssessmentDecisionStatus.PASSED, "评估通过")));

        verify(admissions, never()).decideAssessment(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private Admission pending(long version) {
        return new Admission(3L, 100L, 20L, null, null, AdmissionStatus.PENDING_ASSESSMENT, version, OffsetDateTime.now());
    }
}
