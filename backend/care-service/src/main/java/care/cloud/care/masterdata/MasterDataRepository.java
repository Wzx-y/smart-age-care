package care.cloud.care.masterdata;

import java.util.List;
import java.util.Optional;

public interface MasterDataRepository {
    List<ManagedRoom> findRooms(Long tenantId, boolean includeDisabled);
    Optional<ManagedRoom> findRoom(Long tenantId, Long roomId);
    ManagedRoom createRoom(ManagedRoom room, Long actorId);
    boolean updateRoom(ManagedRoom room);
    boolean changeRoomStatus(ManagedRoom room, boolean enabled);
    long countEnabledBeds(Long tenantId, Long roomId);

    List<ManagedBed> findBeds(Long tenantId, boolean includeDisabled);
    Optional<ManagedBed> findBed(Long tenantId, Long bedId);
    ManagedBed createBed(ManagedBed bed, Long actorId);
    boolean updateBed(ManagedBed bed);
    boolean changeBedStatus(ManagedBed bed, boolean enabled);
    boolean roomIsEnabled(Long tenantId, Long roomId);
    long countCurrentResidentAssignments(Long tenantId, Long bedId);

    List<MasterDataItem> findItems(Long tenantId, MasterDataCategory category, boolean includeDisabled);
    Optional<MasterDataItem> findItem(Long tenantId, MasterDataCategory category, Long itemId);
    MasterDataItem createItem(MasterDataItem item, Long actorId);
    boolean updateItem(MasterDataItem item, Long actorId);
    boolean changeItemStatus(MasterDataItem item, boolean enabled, Long actorId);
    void recordAudit(Long tenantId, Long actorId, String resourceType, Long resourceId, String action);
}
