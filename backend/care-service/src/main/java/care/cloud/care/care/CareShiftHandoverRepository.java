package care.cloud.care.care;

import java.util.List;
import java.util.Optional;

public interface CareShiftHandoverRepository {
    Optional<CareShiftHandover> findByIdAndTenantId(Long id, Long tenantId);
    Optional<CareShiftHandover> findByShiftAndFromUser(Long tenantId, java.time.LocalDate shiftDate, String shiftCode, Long fromUserId);
    List<CareShiftHandover> findByTenantId(Long tenantId);
    List<Long> findTaskIds(Long handoverId, Long tenantId);
    CareShiftHandover create(CareShiftHandover handover);
    void createItems(Long tenantId, Long handoverId, List<Long> taskIds);
    boolean submit(CareShiftHandover handover);
}
