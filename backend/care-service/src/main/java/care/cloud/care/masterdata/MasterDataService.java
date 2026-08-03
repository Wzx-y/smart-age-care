package care.cloud.care.masterdata;

import care.cloud.care.bed.BedHygieneStatus;
import care.cloud.care.bed.BedOccupancyStatus;
import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MasterDataService {
    private final MasterDataRepository repository;

    public MasterDataService(MasterDataRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ManagedRoom> listRooms(boolean includeDisabled) {
        return repository.findRooms(current().tenantId(), includeDisabled);
    }

    public ManagedRoom createRoom(CreateRoomRequest request) {
        TenantPrincipal principal = current();
        try {
            ManagedRoom created = repository.createRoom(new ManagedRoom(null, principal.tenantId(), request.building(),
                    request.floor(), request.roomNo(), request.roomType(), request.nursingUnit(), true, 0L), principal.userId());
            audit(principal, "ROOM", created.id(), "CREATE");
            return created;
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Room number already exists in the current organization");
        }
    }

    public ManagedRoom updateRoom(Long roomId, UpdateRoomRequest request) {
        TenantPrincipal principal = current();
        ManagedRoom existing = room(principal.tenantId(), roomId);
        requireVersion(existing.version(), request.version());
        ManagedRoom updated = new ManagedRoom(existing.id(), existing.tenantId(), request.building(), request.floor(),
                request.roomNo(), request.roomType(), request.nursingUnit(), existing.enabled(), existing.version());
        try {
            if (!repository.updateRoom(updated)) throw conflict("Room was changed by another request");
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Room number already exists in the current organization");
        }
        audit(principal, "ROOM", roomId, "UPDATE");
        return withVersion(updated, updated.version() + 1);
    }

    public ManagedRoom changeRoomStatus(Long roomId, ChangeMasterDataStatusRequest request) {
        TenantPrincipal principal = current();
        ManagedRoom existing = room(principal.tenantId(), roomId);
        requireVersion(existing.version(), request.version());
        if (existing.enabled() == request.enabled()) throw conflict("Room is already in the requested status");
        if (!request.enabled() && repository.countEnabledBeds(principal.tenantId(), roomId) > 0) {
            throw conflict("Disable all beds in the room before disabling the room");
        }
        if (!repository.changeRoomStatus(existing, request.enabled())) throw conflict("Room was changed by another request");
        audit(principal, "ROOM", roomId, request.enabled() ? "ENABLE" : "DISABLE");
        return new ManagedRoom(existing.id(), existing.tenantId(), existing.building(), existing.floor(), existing.roomNo(),
                existing.roomType(), existing.nursingUnit(), request.enabled(), existing.version() + 1);
    }

    @Transactional(readOnly = true)
    public List<ManagedBed> listBeds(boolean includeDisabled) {
        return repository.findBeds(current().tenantId(), includeDisabled);
    }

    public ManagedBed createBed(CreateBedRequest request) {
        TenantPrincipal principal = current();
        if (!repository.roomIsEnabled(principal.tenantId(), request.roomId())) throw new MasterDataNotFoundException("Room", request.roomId());
        try {
            ManagedBed created = repository.createBed(new ManagedBed(null, principal.tenantId(), request.roomId(), null,
                    request.bedNo(), empty(request.equipmentSummary()), BedOccupancyStatus.AVAILABLE, BedHygieneStatus.READY, true, 0L), principal.userId());
            audit(principal, "BED", created.id(), "CREATE");
            return created;
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Bed number already exists in the room");
        }
    }

    public ManagedBed updateBed(Long bedId, UpdateBedRequest request) {
        TenantPrincipal principal = current();
        ManagedBed existing = bed(principal.tenantId(), bedId);
        requireVersion(existing.version(), request.version());
        if (!existing.enabled()) throw conflict("Disabled bed cannot be edited");
        if (!repository.roomIsEnabled(principal.tenantId(), request.roomId())) throw new MasterDataNotFoundException("Room", request.roomId());
        ManagedBed updated = new ManagedBed(existing.id(), existing.tenantId(), request.roomId(), existing.roomNo(), request.bedNo(),
                empty(request.equipmentSummary()), existing.occupancyStatus(), existing.hygieneStatus(), true, existing.version());
        try {
            if (!repository.updateBed(updated)) throw conflict("Only available beds can be edited");
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Bed number already exists in the room");
        }
        audit(principal, "BED", bedId, "UPDATE");
        return withVersion(updated, updated.version() + 1);
    }

    public ManagedBed changeBedStatus(Long bedId, ChangeMasterDataStatusRequest request) {
        TenantPrincipal principal = current();
        ManagedBed existing = bed(principal.tenantId(), bedId);
        requireVersion(existing.version(), request.version());
        if (existing.enabled() == request.enabled()) throw conflict("Bed is already in the requested status");
        if (request.enabled() && !repository.roomIsEnabled(principal.tenantId(), existing.roomId())) {
            throw conflict("Enable the room before enabling its bed");
        }
        if (!request.enabled()) {
            if (existing.occupancyStatus() != BedOccupancyStatus.AVAILABLE || existing.hygieneStatus() != BedHygieneStatus.READY
                    || repository.countCurrentResidentAssignments(principal.tenantId(), bedId) > 0) {
                throw conflict("Only an available, cleaned, unassigned bed can be disabled");
            }
        }
        if (!repository.changeBedStatus(existing, request.enabled())) throw conflict("Bed was changed by another request");
        audit(principal, "BED", bedId, request.enabled() ? "ENABLE" : "DISABLE");
        return new ManagedBed(existing.id(), existing.tenantId(), existing.roomId(), existing.roomNo(), existing.bedNo(),
                existing.equipmentSummary(), existing.occupancyStatus(), existing.hygieneStatus(), request.enabled(), existing.version() + 1);
    }

    @Transactional(readOnly = true)
    public List<MasterDataItem> listItems(MasterDataCategory category, boolean includeDisabled) {
        return repository.findItems(current().tenantId(), category, includeDisabled);
    }

    public MasterDataItem createItem(MasterDataCategory category, CreateMasterDataItemRequest request) {
        TenantPrincipal principal = current();
        try {
            MasterDataItem created = repository.createItem(new MasterDataItem(null, principal.tenantId(), category,
                    request.itemCode(), request.itemName(), empty(request.description()), request.sortOrder(), true, 0L), principal.userId());
            audit(principal, category.name(), created.id(), "CREATE");
            return created;
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Catalog code already exists in the current organization and category");
        }
    }

    public MasterDataItem updateItem(MasterDataCategory category, Long itemId, UpdateMasterDataItemRequest request) {
        TenantPrincipal principal = current();
        MasterDataItem existing = item(principal.tenantId(), category, itemId);
        requireVersion(existing.version(), request.version());
        MasterDataItem updated = new MasterDataItem(existing.id(), existing.tenantId(), category, existing.itemCode(), request.itemName(),
                empty(request.description()), request.sortOrder(), existing.enabled(), existing.version());
        if (!repository.updateItem(updated, principal.userId())) throw conflict("Catalog item was changed by another request");
        audit(principal, category.name(), itemId, "UPDATE");
        return withVersion(updated, updated.version() + 1);
    }

    public MasterDataItem changeItemStatus(MasterDataCategory category, Long itemId, ChangeMasterDataStatusRequest request) {
        TenantPrincipal principal = current();
        MasterDataItem existing = item(principal.tenantId(), category, itemId);
        requireVersion(existing.version(), request.version());
        if (existing.enabled() == request.enabled()) throw conflict("Catalog item is already in the requested status");
        if (!repository.changeItemStatus(existing, request.enabled(), principal.userId())) throw conflict("Catalog item was changed by another request");
        audit(principal, category.name(), itemId, request.enabled() ? "ENABLE" : "DISABLE");
        return new MasterDataItem(existing.id(), existing.tenantId(), existing.category(), existing.itemCode(), existing.itemName(),
                existing.description(), existing.sortOrder(), request.enabled(), existing.version() + 1);
    }

    private TenantPrincipal current() { return TenantContext.requireCurrent(); }
    private ManagedRoom room(Long tenantId, Long roomId) { return repository.findRoom(tenantId, roomId).orElseThrow(() -> new MasterDataNotFoundException("Room", roomId)); }
    private ManagedBed bed(Long tenantId, Long bedId) { return repository.findBed(tenantId, bedId).orElseThrow(() -> new MasterDataNotFoundException("Bed", bedId)); }
    private MasterDataItem item(Long tenantId, MasterDataCategory category, Long itemId) { return repository.findItem(tenantId, category, itemId).orElseThrow(() -> new MasterDataNotFoundException("Catalog item", itemId)); }
    private void audit(TenantPrincipal principal, String resourceType, Long resourceId, String action) { repository.recordAudit(principal.tenantId(), principal.userId(), resourceType, resourceId, action); }
    private void requireVersion(long currentVersion, Long requestedVersion) { if (currentVersion != requestedVersion) throw conflict("Version conflict; refresh and try again"); }
    private MasterDataConflictException conflict(String message) { return new MasterDataConflictException(message); }
    private String empty(String value) { return value == null ? "" : value; }
    private ManagedRoom withVersion(ManagedRoom room, long version) { return new ManagedRoom(room.id(), room.tenantId(), room.building(), room.floor(), room.roomNo(), room.roomType(), room.nursingUnit(), room.enabled(), version); }
    private ManagedBed withVersion(ManagedBed bed, long version) { return new ManagedBed(bed.id(), bed.tenantId(), bed.roomId(), bed.roomNo(), bed.bedNo(), bed.equipmentSummary(), bed.occupancyStatus(), bed.hygieneStatus(), bed.enabled(), version); }
    private MasterDataItem withVersion(MasterDataItem item, long version) { return new MasterDataItem(item.id(), item.tenantId(), item.category(), item.itemCode(), item.itemName(), item.description(), item.sortOrder(), item.enabled(), version); }
}
