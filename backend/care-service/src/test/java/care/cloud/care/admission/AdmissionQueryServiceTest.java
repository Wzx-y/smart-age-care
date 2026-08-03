package care.cloud.care.admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AdmissionQueryServiceTest {
    private final AdmissionRepository admissionRepository = org.mockito.Mockito.mock(AdmissionRepository.class);
    private final AdmissionQueryService service = new AdmissionQueryService(admissionRepository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void listsOnlyAdmissionsInCurrentTenant() {
        TenantContext.set(new TenantPrincipal(801L, 100L));
        AdmissionSummary admission = new AdmissionSummary(
                3L, 20L, "许月琴", null, null, null,
                AdmissionStatus.PENDING_ASSIGNMENT, 2L, null, null, OffsetDateTime.parse("2026-07-29T08:00:00Z"),
                AdmissionAssessmentDecisionStatus.PASSED, "评估通过", OffsetDateTime.parse("2026-07-29T09:00:00Z")
        );
        when(admissionRepository.findByTenantId(100L)).thenReturn(List.of(admission));

        List<AdmissionSummary> result = service.list();

        assertEquals(List.of(admission), result);
        verify(admissionRepository).findByTenantId(100L);
    }
}
