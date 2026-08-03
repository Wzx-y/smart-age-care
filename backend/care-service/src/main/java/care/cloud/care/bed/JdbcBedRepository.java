package care.cloud.care.bed;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBedRepository implements BedRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<Bed> rowMapper = (resultSet, rowNum) -> new Bed(
            resultSet.getLong("id"),
            resultSet.getLong("tenant_id"),
            resultSet.getLong("room_id"),
            resultSet.getString("bed_no"),
            BedOccupancyStatus.valueOf(resultSet.getString("occupancy_status")),
            BedHygieneStatus.valueOf(resultSet.getString("hygiene_status")),
            resultSet.getLong("version")
    );
    private final RowMapper<BedSummary> summaryRowMapper = (resultSet, rowNum) -> new BedSummary(
            resultSet.getLong("id"),
            resultSet.getLong("room_id"),
            resultSet.getString("room_no"),
            resultSet.getString("room_type"),
            resultSet.getString("bed_no"),
            BedOccupancyStatus.valueOf(resultSet.getString("occupancy_status")),
            BedHygieneStatus.valueOf(resultSet.getString("hygiene_status")),
            resultSet.getLong("version")
    );

    public JdbcBedRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Bed> findByIdAndTenantId(Long id, Long tenantId) {
        List<Bed> beds = jdbcTemplate.query("""
                select bed.id, bed.tenant_id, bed.room_id, bed.bed_no, bed.occupancy_status, bed.hygiene_status, bed.version
                from care_bed bed
                join care_room room on room.id = bed.room_id and room.tenant_id = bed.tenant_id
                where bed.id = :id and bed.tenant_id = :tenantId
                  and bed.enabled = '0' and room.enabled = '0'
                """, new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId), rowMapper);
        return beds.stream().findFirst();
    }

    @Override
    public List<BedSummary> findByTenantId(Long tenantId, BedOccupancyStatus occupancyStatus) {
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("tenantId", tenantId);
        String occupancyClause = "";
        if (occupancyStatus != null) {
            occupancyClause = " and bed.occupancy_status = :occupancyStatus";
            parameters.addValue("occupancyStatus", occupancyStatus.name());
        }
        return jdbcTemplate.query(("""
                select bed.id, bed.room_id, room.room_no, room.room_type, bed.bed_no,
                       bed.occupancy_status, bed.hygiene_status, bed.version
                from care_bed bed
                join care_room room on room.id = bed.room_id and room.tenant_id = bed.tenant_id
                where bed.tenant_id = :tenantId
                  and bed.enabled = '0'
                  and room.enabled = '0'%s
                order by room.room_no, bed.bed_no
                """).formatted(occupancyClause), parameters, summaryRowMapper);
    }

    @Override
    public boolean reserve(Bed bed) {
        int updated = jdbcTemplate.update("""
                update care_bed
                set occupancy_status = 'RESERVED', version = version + 1
                where id = :id
                  and tenant_id = :tenantId
                  and enabled = '0'
                  and occupancy_status = 'AVAILABLE'
                  and hygiene_status = 'READY'
                  and version = :version
                """, new MapSqlParameterSource()
                .addValue("id", bed.id())
                .addValue("tenantId", bed.tenantId())
                .addValue("version", bed.version()));
        return updated == 1;
    }

    @Override
    public boolean occupy(Bed bed) {
        int updated = jdbcTemplate.update("""
                update care_bed
                set occupancy_status = 'OCCUPIED', version = version + 1
                where id = :id
                  and tenant_id = :tenantId
                  and enabled = '0'
                  and occupancy_status = 'RESERVED'
                  and hygiene_status = 'READY'
                  and version = :version
                """, new MapSqlParameterSource()
                .addValue("id", bed.id())
                .addValue("tenantId", bed.tenantId())
                .addValue("version", bed.version()));
        return updated == 1;
    }

    @Override
    public boolean occupyAvailable(Bed bed) {
        return jdbcTemplate.update("""
                update care_bed
                set occupancy_status = 'OCCUPIED', version = version + 1
                where id = :id and tenant_id = :tenantId and enabled = '0'
                  and occupancy_status = 'AVAILABLE' and hygiene_status = 'READY' and version = :version
                """, new MapSqlParameterSource().addValue("id", bed.id()).addValue("tenantId", bed.tenantId())
                .addValue("version", bed.version())) == 1;
    }

    @Override
    public boolean releaseForCleaning(Bed bed) {
        int updated = jdbcTemplate.update("""
                update care_bed
                set occupancy_status = 'CLEANING', version = version + 1
                where id = :id
                  and tenant_id = :tenantId
                  and enabled = '0'
                  and occupancy_status = 'OCCUPIED'
                  and version = :version
                """, new MapSqlParameterSource()
                .addValue("id", bed.id())
                .addValue("tenantId", bed.tenantId())
                .addValue("version", bed.version()));
        return updated == 1;
    }

    @Override
    public boolean completeCleaning(Bed bed) {
        int updated = jdbcTemplate.update("""
                update care_bed
                set occupancy_status = 'AVAILABLE', hygiene_status = 'READY', version = version + 1
                where id = :id and tenant_id = :tenantId and enabled = '0'
                  and occupancy_status = 'CLEANING' and version = :version
                """, new MapSqlParameterSource()
                .addValue("id", bed.id())
                .addValue("tenantId", bed.tenantId())
                .addValue("version", bed.version()));
        return updated == 1;
    }

    @Override
    public void recordCleaningResult(Bed bed, String result, Long actorId) {
        jdbcTemplate.update("""
                insert into care_bed_cleaning_record (tenant_id, bed_id, result, cleaned_by, cleaned_at)
                values (:tenantId, :bedId, :result, :actorId, current_timestamp(3))
                """, new MapSqlParameterSource().addValue("tenantId", bed.tenantId()).addValue("bedId", bed.id())
                .addValue("result", result).addValue("actorId", actorId));
    }

    @Override
    public boolean releaseReservation(Bed bed) {
        return jdbcTemplate.update("""
                update care_bed set occupancy_status = 'AVAILABLE', version = version + 1
                where id = :id and tenant_id = :tenantId and enabled = '0' and occupancy_status = 'RESERVED'
                  and hygiene_status = 'READY' and version = :version
                """, new MapSqlParameterSource().addValue("id", bed.id()).addValue("tenantId", bed.tenantId())
                .addValue("version", bed.version())) == 1;
    }
}
