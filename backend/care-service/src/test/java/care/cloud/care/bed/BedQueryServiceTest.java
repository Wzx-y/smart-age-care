package care.cloud.care.bed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BedQueryServiceTest {
    private final BedRepository bedRepository = org.mockito.Mockito.mock(BedRepository.class);
    private final BedQueryService service = new BedQueryService(bedRepository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void listsCurrentTenantBedsWithStatusFilter() {
        TenantContext.set(new TenantPrincipal(801L, 100L));
        BedSummary bed = new BedSummary(
                7L, 2L, "A-205", "双人照护房", "A-205-02",
                BedOccupancyStatus.AVAILABLE, BedHygieneStatus.READY, 5L
        );
        when(bedRepository.findByTenantId(100L, BedOccupancyStatus.AVAILABLE)).thenReturn(List.of(bed));

        List<BedSummary> result = service.list(BedOccupancyStatus.AVAILABLE);

        assertEquals(List.of(bed), result);
        verify(bedRepository).findByTenantId(100L, BedOccupancyStatus.AVAILABLE);
    }
}
