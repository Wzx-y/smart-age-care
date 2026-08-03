package care.cloud.care.resident;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository {
    Resident save(Resident resident);

    boolean update(Resident resident);

    boolean archive(Long id, Long tenantId, java.time.OffsetDateTime archivedAt);

    Optional<Resident> findByIdAndTenantId(Long id, Long tenantId);

    List<Resident> findByTenantId(Long tenantId, String keyword);
}
