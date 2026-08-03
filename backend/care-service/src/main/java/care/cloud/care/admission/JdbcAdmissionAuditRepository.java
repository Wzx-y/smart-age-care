package care.cloud.care.admission;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAdmissionAuditRepository implements AdmissionAuditRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAdmissionAuditRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void recordConfirmed(Long tenantId, Long actorId, Long admissionId, Long residentId, Long bedId) {
        jdbcTemplate.update("""
                insert into care_audit_log (
                    tenant_id, actor_id, action, target_type, target_id, related_resident_id, related_bed_id, created_at
                ) values (
                    :tenantId, :actorId, 'ADMISSION_CONFIRMED', 'ADMISSION', :admissionId, :residentId, :bedId, current_timestamp(3)
                )
                """, new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("actorId", actorId)
                .addValue("admissionId", admissionId)
                .addValue("residentId", residentId)
                .addValue("bedId", bedId));
    }

    @Override
    public void recordDischarged(Long tenantId, Long actorId, Long admissionId, Long residentId, Long bedId) {
        jdbcTemplate.update("""
                insert into care_audit_log (
                    tenant_id, actor_id, action, target_type, target_id, related_resident_id, related_bed_id, created_at
                ) values (
                    :tenantId, :actorId, 'ADMISSION_DISCHARGED', 'ADMISSION', :admissionId, :residentId, :bedId, current_timestamp(3)
                )
                """, new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("actorId", actorId)
                .addValue("admissionId", admissionId)
                .addValue("residentId", residentId)
                .addValue("bedId", bedId));
    }

    @Override
    public void recordCancelled(Long tenantId, Long actorId, Long admissionId, Long residentId, Long bedId) {
        jdbcTemplate.update("""
                insert into care_audit_log (tenant_id, actor_id, action, target_type, target_id, related_resident_id, related_bed_id, created_at)
                values (:tenantId, :actorId, 'ADMISSION_CANCELLED', 'ADMISSION', :admissionId, :residentId, :bedId, current_timestamp(3))
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("actorId", actorId)
                .addValue("admissionId", admissionId).addValue("residentId", residentId).addValue("bedId", bedId));
    }

    @Override
    public void recordReservationExpired(Long tenantId, Long admissionId, Long residentId, Long bedId) {
        jdbcTemplate.update("""
                insert into care_audit_log (tenant_id, actor_id, action, target_type, target_id, related_resident_id, related_bed_id, created_at)
                values (:tenantId, 0, 'ADMISSION_RESERVATION_EXPIRED', 'ADMISSION', :admissionId, :residentId, :bedId, current_timestamp(3))
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("admissionId", admissionId)
                .addValue("residentId", residentId).addValue("bedId", bedId));
    }

    @Override
    public void recordAssessmentDecided(Long tenantId, Long actorId, Long admissionId, Long residentId,
                                        AdmissionAssessmentDecisionStatus decision) {
        record(tenantId, actorId, "ADMISSION_ASSESSMENT_" + decision.name(), admissionId, residentId, null);
    }

    @Override
    public void recordBedTransferred(Long tenantId, Long actorId, Long admissionId, Long residentId,
                                     Long sourceBedId, Long targetBedId) {
        record(tenantId, actorId, "ADMISSION_BED_TRANSFERRED", admissionId, residentId, targetBedId);
    }

    @Override
    public void recordCleaningCompleted(Long tenantId, Long actorId, Long bedId) {
        jdbcTemplate.update("""
                insert into care_audit_log (tenant_id, actor_id, action, target_type, target_id, related_bed_id, created_at)
                values (:tenantId, :actorId, 'BED_CLEANING_COMPLETED', 'BED', :bedId, :bedId, current_timestamp(3))
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("actorId", actorId).addValue("bedId", bedId));
    }

    private void record(Long tenantId, Long actorId, String action, Long admissionId, Long residentId, Long bedId) {
        jdbcTemplate.update("""
                insert into care_audit_log (tenant_id, actor_id, action, target_type, target_id, related_resident_id, related_bed_id, created_at)
                values (:tenantId, :actorId, :action, 'ADMISSION', :admissionId, :residentId, :bedId, current_timestamp(3))
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("actorId", actorId)
                .addValue("action", action).addValue("admissionId", admissionId).addValue("residentId", residentId).addValue("bedId", bedId));
    }
}
