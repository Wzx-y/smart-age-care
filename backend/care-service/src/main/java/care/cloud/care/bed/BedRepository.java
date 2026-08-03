package care.cloud.care.bed;

import java.util.List;
import java.util.Optional;

public interface BedRepository {
    Optional<Bed> findByIdAndTenantId(Long id, Long tenantId);

    List<BedSummary> findByTenantId(Long tenantId, BedOccupancyStatus occupancyStatus);

    boolean reserve(Bed bed);

    boolean occupy(Bed bed);

    boolean occupyAvailable(Bed bed);

    boolean releaseForCleaning(Bed bed);

    boolean completeCleaning(Bed bed);

    void recordCleaningResult(Bed bed, String result, Long actorId);

    boolean releaseReservation(Bed bed);
}
