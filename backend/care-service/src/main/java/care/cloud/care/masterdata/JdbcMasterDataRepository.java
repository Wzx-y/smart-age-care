package care.cloud.care.masterdata;

import care.cloud.care.bed.BedHygieneStatus;
import care.cloud.care.bed.BedOccupancyStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMasterDataRepository implements MasterDataRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<ManagedRoom> roomRowMapper = (row, index) -> new ManagedRoom(
            row.getLong("id"), row.getLong("tenant_id"), row.getString("building"), row.getString("floor"),
            row.getString("room_no"), row.getString("room_type"), row.getString("nursing_unit"),
            "0".equals(row.getString("enabled")), row.getLong("version"));
    private final RowMapper<ManagedBed> bedRowMapper = (row, index) -> new ManagedBed(
            row.getLong("id"), row.getLong("tenant_id"), row.getLong("room_id"), row.getString("room_no"),
            row.getString("bed_no"), row.getString("equipment_summary"),
            BedOccupancyStatus.valueOf(row.getString("occupancy_status")),
            BedHygieneStatus.valueOf(row.getString("hygiene_status")), "0".equals(row.getString("enabled")), row.getLong("version"));
    private final RowMapper<MasterDataItem> itemRowMapper = (row, index) -> new MasterDataItem(
            row.getLong("id"), row.getLong("tenant_id"), MasterDataCategory.valueOf(row.getString("category")),
            row.getString("item_code"), row.getString("item_name"), row.getString("description"),
            row.getInt("sort_order"), "0".equals(row.getString("enabled")), row.getLong("version"));

    public JdbcMasterDataRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override public List<ManagedRoom> findRooms(Long tenantId, boolean includeDisabled) {
        String disabledClause = includeDisabled ? "" : " and enabled = '0'";
        return jdbcTemplate.query(("select id, tenant_id, building, floor, room_no, room_type, nursing_unit, enabled, version from care_room where tenant_id = :tenantId" + disabledClause + " order by building, floor, room_no"), params(tenantId), roomRowMapper);
    }
    @Override public Optional<ManagedRoom> findRoom(Long tenantId, Long roomId) {
        return jdbcTemplate.query("select id, tenant_id, building, floor, room_no, room_type, nursing_unit, enabled, version from care_room where tenant_id = :tenantId and id = :id", params(tenantId).addValue("id", roomId), roomRowMapper).stream().findFirst();
    }
    @Override public ManagedRoom createRoom(ManagedRoom room, Long actorId) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder(); OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("insert into care_room (tenant_id, building, floor, room_no, room_type, nursing_unit, enabled, version, created_at, updated_at) values (:tenantId, :building, :floor, :roomNo, :roomType, :nursingUnit, '0', 0, :now, :now)", roomParams(room).addValue("now", now), keys, new String[] { "id" });
        return new ManagedRoom(keys.getKey().longValue(), room.tenantId(), room.building(), room.floor(), room.roomNo(), room.roomType(), room.nursingUnit(), true, 0L);
    }
    @Override public boolean updateRoom(ManagedRoom room) {
        return jdbcTemplate.update("update care_room set building = :building, floor = :floor, room_no = :roomNo, room_type = :roomType, nursing_unit = :nursingUnit, version = version + 1, updated_at = current_timestamp(3) where id = :id and tenant_id = :tenantId and version = :version", roomParams(room)) == 1;
    }
    @Override public boolean changeRoomStatus(ManagedRoom room, boolean enabled) {
        return jdbcTemplate.update("update care_room set enabled = :enabled, version = version + 1, updated_at = current_timestamp(3) where id = :id and tenant_id = :tenantId and version = :version", params(room.tenantId()).addValue("id", room.id()).addValue("version", room.version()).addValue("enabled", enabled ? "0" : "1")) == 1;
    }
    @Override public long countEnabledBeds(Long tenantId, Long roomId) { return jdbcTemplate.queryForObject("select count(1) from care_bed where tenant_id = :tenantId and room_id = :roomId and enabled = '0'", params(tenantId).addValue("roomId", roomId), Long.class); }

    @Override public List<ManagedBed> findBeds(Long tenantId, boolean includeDisabled) {
        String disabledClause = includeDisabled ? "" : " and bed.enabled = '0' and room.enabled = '0'";
        return jdbcTemplate.query(("select bed.id, bed.tenant_id, bed.room_id, room.room_no, bed.bed_no, bed.equipment_summary, bed.occupancy_status, bed.hygiene_status, bed.enabled, bed.version from care_bed bed join care_room room on room.id = bed.room_id and room.tenant_id = bed.tenant_id where bed.tenant_id = :tenantId" + disabledClause + " order by room.room_no, bed.bed_no"), params(tenantId), bedRowMapper);
    }
    @Override public Optional<ManagedBed> findBed(Long tenantId, Long bedId) {
        return jdbcTemplate.query("select bed.id, bed.tenant_id, bed.room_id, room.room_no, bed.bed_no, bed.equipment_summary, bed.occupancy_status, bed.hygiene_status, bed.enabled, bed.version from care_bed bed join care_room room on room.id = bed.room_id and room.tenant_id = bed.tenant_id where bed.tenant_id = :tenantId and bed.id = :id", params(tenantId).addValue("id", bedId), bedRowMapper).stream().findFirst();
    }
    @Override public ManagedBed createBed(ManagedBed bed, Long actorId) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder(); OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("insert into care_bed (tenant_id, room_id, bed_no, equipment_summary, occupancy_status, hygiene_status, enabled, version, created_at, updated_at) values (:tenantId, :roomId, :bedNo, :equipmentSummary, 'AVAILABLE', 'READY', '0', 0, :now, :now)", bedParams(bed).addValue("now", now), keys, new String[] { "id" });
        return new ManagedBed(keys.getKey().longValue(), bed.tenantId(), bed.roomId(), bed.roomNo(), bed.bedNo(), bed.equipmentSummary(), BedOccupancyStatus.AVAILABLE, BedHygieneStatus.READY, true, 0L);
    }
    @Override public boolean updateBed(ManagedBed bed) {
        return jdbcTemplate.update("update care_bed set room_id = :roomId, bed_no = :bedNo, equipment_summary = :equipmentSummary, version = version + 1, updated_at = current_timestamp(3) where id = :id and tenant_id = :tenantId and enabled = '0' and occupancy_status = 'AVAILABLE' and version = :version", bedParams(bed)) == 1;
    }
    @Override public boolean changeBedStatus(ManagedBed bed, boolean enabled) {
        return jdbcTemplate.update("update care_bed set enabled = :enabled, version = version + 1, updated_at = current_timestamp(3) where id = :id and tenant_id = :tenantId and occupancy_status = 'AVAILABLE' and hygiene_status = 'READY' and version = :version", params(bed.tenantId()).addValue("id", bed.id()).addValue("version", bed.version()).addValue("enabled", enabled ? "0" : "1")) == 1;
    }
    @Override public boolean roomIsEnabled(Long tenantId, Long roomId) { return jdbcTemplate.queryForObject("select count(1) from care_room where tenant_id = :tenantId and id = :roomId and enabled = '0'", params(tenantId).addValue("roomId", roomId), Long.class) == 1L; }
    @Override public long countCurrentResidentAssignments(Long tenantId, Long bedId) { return jdbcTemplate.queryForObject("select count(1) from care_resident where tenant_id = :tenantId and current_bed_id = :bedId and archived_at is null", params(tenantId).addValue("bedId", bedId), Long.class); }

    @Override public List<MasterDataItem> findItems(Long tenantId, MasterDataCategory category, boolean includeDisabled) {
        String disabledClause = includeDisabled ? "" : " and enabled = '0'";
        return jdbcTemplate.query(("select id, tenant_id, category, item_code, item_name, description, sort_order, enabled, version from care_master_data_item where tenant_id = :tenantId and category = :category" + disabledClause + " order by sort_order, item_code"), params(tenantId).addValue("category", category.name()), itemRowMapper);
    }
    @Override public Optional<MasterDataItem> findItem(Long tenantId, MasterDataCategory category, Long itemId) { return jdbcTemplate.query("select id, tenant_id, category, item_code, item_name, description, sort_order, enabled, version from care_master_data_item where tenant_id = :tenantId and category = :category and id = :id", params(tenantId).addValue("category", category.name()).addValue("id", itemId), itemRowMapper).stream().findFirst(); }
    @Override public MasterDataItem createItem(MasterDataItem item, Long actorId) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder(); OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("insert into care_master_data_item (tenant_id, category, item_code, item_name, description, sort_order, enabled, version, created_by, updated_by, created_at, updated_at) values (:tenantId, :category, :itemCode, :itemName, :description, :sortOrder, '0', 0, :actorId, :actorId, :now, :now)", itemParams(item).addValue("actorId", actorId).addValue("now", now), keys, new String[] { "id" });
        return new MasterDataItem(keys.getKey().longValue(), item.tenantId(), item.category(), item.itemCode(), item.itemName(), item.description(), item.sortOrder(), true, 0L);
    }
    @Override public boolean updateItem(MasterDataItem item, Long actorId) { return jdbcTemplate.update("update care_master_data_item set item_name = :itemName, description = :description, sort_order = :sortOrder, updated_by = :actorId, updated_at = current_timestamp(3), version = version + 1 where id = :id and tenant_id = :tenantId and category = :category and version = :version", itemParams(item).addValue("actorId", actorId)) == 1; }
    @Override public boolean changeItemStatus(MasterDataItem item, boolean enabled, Long actorId) { return jdbcTemplate.update("update care_master_data_item set enabled = :enabled, updated_by = :actorId, updated_at = current_timestamp(3), version = version + 1 where id = :id and tenant_id = :tenantId and category = :category and version = :version", itemParams(item).addValue("actorId", actorId).addValue("enabled", enabled ? "0" : "1")) == 1; }
    @Override public void recordAudit(Long tenantId, Long actorId, String resourceType, Long resourceId, String action) { jdbcTemplate.update("insert into care_master_data_audit (tenant_id, actor_id, resource_type, resource_id, action, created_at) values (:tenantId, :actorId, :resourceType, :resourceId, :action, current_timestamp(3))", params(tenantId).addValue("actorId", actorId).addValue("resourceType", resourceType).addValue("resourceId", resourceId).addValue("action", action)); }

    private MapSqlParameterSource params(Long tenantId) { return new MapSqlParameterSource().addValue("tenantId", tenantId); }
    private MapSqlParameterSource roomParams(ManagedRoom room) { return params(room.tenantId()).addValue("id", room.id()).addValue("building", room.building()).addValue("floor", room.floor()).addValue("roomNo", room.roomNo()).addValue("roomType", room.roomType()).addValue("nursingUnit", room.nursingUnit()).addValue("version", room.version()); }
    private MapSqlParameterSource bedParams(ManagedBed bed) { return params(bed.tenantId()).addValue("id", bed.id()).addValue("roomId", bed.roomId()).addValue("bedNo", bed.bedNo()).addValue("equipmentSummary", bed.equipmentSummary()).addValue("version", bed.version()); }
    private MapSqlParameterSource itemParams(MasterDataItem item) { return params(item.tenantId()).addValue("id", item.id()).addValue("category", item.category().name()).addValue("itemCode", item.itemCode()).addValue("itemName", item.itemName()).addValue("description", item.description()).addValue("sortOrder", item.sortOrder()).addValue("version", item.version()); }
}
