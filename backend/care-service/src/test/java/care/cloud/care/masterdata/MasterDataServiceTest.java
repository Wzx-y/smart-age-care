package care.cloud.care.masterdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.bed.BedHygieneStatus;
import care.cloud.care.bed.BedOccupancyStatus;
import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

class MasterDataServiceTest {
    private final MasterDataRepository repository = Mockito.mock(MasterDataRepository.class);
    private final MasterDataService service = new MasterDataService(repository);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void roomListUsesOnlyTheCurrentTenant() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(repository.findRooms(12L, false)).thenReturn(List.of());

        service.listRooms(false);

        verify(repository).findRooms(12L, false);
    }

    @Test
    void createRoomUsesTrustedTenantAndAuditsTheCreatedRoom() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(repository.createRoom(any(ManagedRoom.class), eq(45L))).thenReturn(room(71L, 0L, true));

        ManagedRoom created = service.createRoom(new CreateRoomRequest("A", "3", "301", "双人间", "失能照护区"));

        ArgumentCaptor<ManagedRoom> roomCaptor = ArgumentCaptor.forClass(ManagedRoom.class);
        verify(repository).createRoom(roomCaptor.capture(), eq(45L));
        assertEquals(12L, roomCaptor.getValue().tenantId());
        assertEquals(71L, created.id());
        verify(repository).recordAudit(12L, 45L, "ROOM", 71L, "CREATE");
    }

    @Test
    void duplicateRoomNumberBecomesAConflict() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(repository.createRoom(any(ManagedRoom.class), eq(45L))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThrows(MasterDataConflictException.class,
                () -> service.createRoom(new CreateRoomRequest("A", "3", "301", "双人间", "失能照护区")));
    }

    @Test
    void staleRoomVersionDoesNotWriteOrAudit() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(repository.findRoom(12L, 71L)).thenReturn(Optional.of(room(71L, 4L, true)));

        assertThrows(MasterDataConflictException.class,
                () -> service.updateRoom(71L, new UpdateRoomRequest(3L, "A", "3", "301", "双人间", "失能照护区")));

        verify(repository, never()).updateRoom(any());
        verify(repository, never()).recordAudit(any(), any(), any(), any(), any());
    }

    @Test
    void roomWithEnabledBedsCannotBeDisabled() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        when(repository.findRoom(12L, 71L)).thenReturn(Optional.of(room(71L, 4L, true)));
        when(repository.countEnabledBeds(12L, 71L)).thenReturn(1L);

        assertThrows(MasterDataConflictException.class,
                () -> service.changeRoomStatus(71L, new ChangeMasterDataStatusRequest(4L, false)));

        verify(repository, never()).changeRoomStatus(any(), eq(false));
    }

    @Test
    void occupiedOrResidentAssignedBedCannotBeDisabled() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        ManagedBed occupied = new ManagedBed(81L, 12L, 71L, "301", "A", "护理呼叫器",
                BedOccupancyStatus.OCCUPIED, BedHygieneStatus.READY, true, 3L);
        when(repository.findBed(12L, 81L)).thenReturn(Optional.of(occupied));

        assertThrows(MasterDataConflictException.class,
                () -> service.changeBedStatus(81L, new ChangeMasterDataStatusRequest(3L, false)));

        verify(repository, never()).changeBedStatus(any(), eq(false));
    }

    @Test
    void catalogStatusChangeUsesVersionAndWritesAudit() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        MasterDataItem item = new MasterDataItem(91L, 12L, MasterDataCategory.RISK_LEVEL, "HIGH", "高风险", "重点关注", 10, true, 2L);
        when(repository.findItem(12L, MasterDataCategory.RISK_LEVEL, 91L)).thenReturn(Optional.of(item));
        when(repository.changeItemStatus(item, false, 45L)).thenReturn(true);

        MasterDataItem changed = service.changeItemStatus(MasterDataCategory.RISK_LEVEL, 91L, new ChangeMasterDataStatusRequest(2L, false));

        assertEquals(false, changed.enabled());
        assertEquals(3L, changed.version());
        verify(repository).recordAudit(12L, 45L, "RISK_LEVEL", 91L, "DISABLE");
    }

    private ManagedRoom room(Long id, long version, boolean enabled) {
        return new ManagedRoom(id, 12L, "A", "3", "301", "双人间", "失能照护区", enabled, version);
    }
}
