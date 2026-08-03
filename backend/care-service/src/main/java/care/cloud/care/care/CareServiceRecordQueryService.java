package care.cloud.care.care;

import care.cloud.care.security.TenantContext;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CareServiceRecordQueryService {
    private final CareTaskRepository repository;

    public CareServiceRecordQueryService(CareTaskRepository repository) {
        this.repository = repository;
    }

    public List<CareServiceRecord> list(Long residentId, LocalDate serviceDate, Long executorId) {
        return repository.findServiceRecords(TenantContext.requireCurrent().tenantId(), residentId, serviceDate, executorId);
    }
}
