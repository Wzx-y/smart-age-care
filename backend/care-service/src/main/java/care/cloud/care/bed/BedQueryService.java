package care.cloud.care.bed;

import care.cloud.care.security.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BedQueryService {
    private final BedRepository bedRepository;

    public BedQueryService(BedRepository bedRepository) {
        this.bedRepository = bedRepository;
    }

    public List<BedSummary> list(BedOccupancyStatus occupancyStatus) {
        return bedRepository.findByTenantId(TenantContext.requireCurrent().tenantId(), occupancyStatus);
    }
}
