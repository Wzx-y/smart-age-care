package care.cloud.care.resident;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcResidentAdmissionRepository implements ResidentAdmissionRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcResidentAdmissionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean markAdmitted(Long residentId, Long tenantId, Long bedId, Long userId) {
        int updated = jdbcTemplate.update("""
                update care_resident
                set status = 'IN_RESIDENCE',
                    current_bed_id = :bedId,
                    updated_at = current_timestamp(3)
                where id = :residentId
                  and tenant_id = :tenantId
                  and archived_at is null
                  and status = 'PENDING_ADMISSION'
                  and current_bed_id is null
                """, new MapSqlParameterSource()
                .addValue("residentId", residentId)
                .addValue("tenantId", tenantId)
                .addValue("bedId", bedId)
                .addValue("userId", userId));
        return updated == 1;
    }

    @Override
    public boolean markDischarged(Long residentId, Long tenantId, Long bedId, Long userId) {
        int updated = jdbcTemplate.update("""
                update care_resident
                set status = 'DISCHARGED',
                    current_bed_id = null,
                    updated_at = current_timestamp(3)
                where id = :residentId
                  and tenant_id = :tenantId
                  and archived_at is null
                  and status = 'IN_RESIDENCE'
                  and current_bed_id = :bedId
                """, new MapSqlParameterSource()
                .addValue("residentId", residentId)
                .addValue("tenantId", tenantId)
                .addValue("bedId", bedId)
                .addValue("userId", userId));
        return updated == 1;
    }

    @Override
    public boolean moveBed(Long residentId, Long tenantId, Long sourceBedId, Long targetBedId, Long userId) {
        return jdbcTemplate.update("""
                update care_resident set current_bed_id = :targetBedId, updated_at = current_timestamp(3)
                where id = :residentId and tenant_id = :tenantId and archived_at is null
                  and status = 'IN_RESIDENCE' and current_bed_id = :sourceBedId
                """, new MapSqlParameterSource().addValue("residentId", residentId).addValue("tenantId", tenantId)
                .addValue("sourceBedId", sourceBedId).addValue("targetBedId", targetBedId).addValue("userId", userId)) == 1;
    }
}
