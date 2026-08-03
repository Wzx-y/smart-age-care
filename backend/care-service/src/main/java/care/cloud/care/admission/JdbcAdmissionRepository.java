package care.cloud.care.admission;

import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAdmissionRepository implements AdmissionRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<Admission> rowMapper = (resultSet, rowNum) -> new Admission(
            resultSet.getLong("id"),
            resultSet.getLong("tenant_id"),
            resultSet.getLong("resident_id"),
            resultSet.getObject("bed_id", Long.class),
            resultSet.getTimestamp("reserved_until") == null ? null : resultSet.getTimestamp("reserved_until").toInstant().atOffset(ZoneOffset.UTC),
            AdmissionStatus.valueOf(resultSet.getString("status")),
            resultSet.getLong("version"),
            resultSet.getTimestamp("applied_at").toInstant().atOffset(ZoneOffset.UTC)
    );
    private final RowMapper<AdmissionSummary> summaryRowMapper = (resultSet, rowNum) -> new AdmissionSummary(
            resultSet.getLong("id"),
            resultSet.getLong("resident_id"),
            resultSet.getString("resident_name"),
            resultSet.getObject("bed_id", Long.class),
            resultSet.getString("room_no"),
            resultSet.getString("bed_no"),
            AdmissionStatus.valueOf(resultSet.getString("status")),
            resultSet.getLong("version"),
            resultSet.getObject("bed_version", Long.class),
            resultSet.getTimestamp("reserved_until") == null ? null : resultSet.getTimestamp("reserved_until").toInstant().atOffset(ZoneOffset.UTC),
            resultSet.getTimestamp("applied_at").toInstant().atOffset(ZoneOffset.UTC),
            resultSet.getString("assessment_decision") == null ? null : AdmissionAssessmentDecisionStatus.valueOf(resultSet.getString("assessment_decision")),
            resultSet.getString("assessment_conclusion"),
            resultSet.getTimestamp("assessed_at") == null ? null : resultSet.getTimestamp("assessed_at").toInstant().atOffset(ZoneOffset.UTC)
    );

    public JdbcAdmissionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Admission create(Long tenantId, Long residentId, Long userId) {
        var parameters = new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("residentId", residentId).addValue("userId", userId);
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into care_admission (tenant_id, resident_id, status, version, applied_at, created_by, updated_by, created_at, updated_at)
                values (:tenantId, :residentId, 'PENDING_ASSESSMENT', 0, current_timestamp(3), :userId, :userId, current_timestamp(3), current_timestamp(3))
                """, parameters, keyHolder, new String[]{"id"});
        Number id = keyHolder.getKey();
        if (id == null) throw new IllegalStateException("创建入住申请后未返回主键");
        return findByIdAndTenantId(id.longValue(), tenantId).orElseThrow();
    }

    @Override
    public Optional<Admission> findByIdAndTenantId(Long id, Long tenantId) {
        List<Admission> admissions = jdbcTemplate.query("""
                select id, tenant_id, resident_id, bed_id, reserved_until, status, version, applied_at
                from care_admission
                where id = :id and tenant_id = :tenantId
                """, new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId), rowMapper);
        return admissions.stream().findFirst();
    }

    @Override
    public List<AdmissionSummary> findByTenantId(Long tenantId) {
        return jdbcTemplate.query("""
                select a.id, a.resident_id, r.name as resident_name, a.bed_id,
                       room.room_no, bed.bed_no, a.status, a.version, bed.version as bed_version, a.reserved_until, a.applied_at,
                       assessment.decision as assessment_decision, assessment.conclusion as assessment_conclusion, assessment.assessed_at
                from care_admission a
                join care_resident r on r.id = a.resident_id and r.tenant_id = a.tenant_id and r.archived_at is null
                left join care_bed bed on bed.id = a.bed_id and bed.tenant_id = a.tenant_id
                left join care_room room on room.id = bed.room_id and room.tenant_id = a.tenant_id
                left join care_admission_assessment assessment on assessment.id = (
                    select newest.id from care_admission_assessment newest
                    where newest.tenant_id = a.tenant_id and newest.admission_id = a.id
                    order by newest.assessed_at desc, newest.id desc limit 1
                )
                where a.tenant_id = :tenantId
                order by a.applied_at desc, a.id desc
                """, new MapSqlParameterSource().addValue("tenantId", tenantId), summaryRowMapper);
    }

    @Override
    public boolean assignBed(Admission admission, Long bedId, OffsetDateTime reservedUntil, Long userId) {
        int updated = jdbcTemplate.update("""
                update care_admission
                set bed_id = :bedId,
                    reserved_until = :reservedUntil,
                    status = 'PENDING_CONFIRMATION',
                    version = version + 1,
                    updated_by = :userId,
                    updated_at = current_timestamp(3)
                where id = :id
                  and tenant_id = :tenantId
                  and status = 'PENDING_ASSIGNMENT'
                  and version = :version
                """, new MapSqlParameterSource()
                .addValue("id", admission.id())
                .addValue("tenantId", admission.tenantId())
                .addValue("bedId", bedId)
                .addValue("reservedUntil", java.sql.Timestamp.from(reservedUntil.toInstant()))
                .addValue("userId", userId)
                .addValue("version", admission.version()));
        return updated == 1;
    }

    @Override
    public boolean decideAssessment(Admission admission, AdmissionStatus nextStatus,
                                    DecideAdmissionAssessmentRequest request, Long userId) {
        int updated = jdbcTemplate.update("""
                update care_admission set status = :nextStatus, version = version + 1,
                    updated_by = :userId, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId
                  and status in ('PENDING_ASSESSMENT', 'ASSESSMENT_REJECTED') and version = :version
                """, new MapSqlParameterSource().addValue("id", admission.id()).addValue("tenantId", admission.tenantId())
                .addValue("nextStatus", nextStatus.name()).addValue("userId", userId).addValue("version", admission.version()));
        if (updated != 1) return false;
        jdbcTemplate.update("""
                insert into care_admission_assessment (tenant_id, admission_id, decision, conclusion, assessed_by, assessed_at)
                values (:tenantId, :admissionId, :decision, :conclusion, :assessedBy, current_timestamp(3))
                """, new MapSqlParameterSource().addValue("tenantId", admission.tenantId()).addValue("admissionId", admission.id())
                .addValue("decision", request.decision().name()).addValue("conclusion", request.conclusion()).addValue("assessedBy", userId));
        return true;
    }

    @Override
    public boolean confirm(Admission admission, Long userId) {
        int updated = jdbcTemplate.update("""
                update care_admission
                set status = 'ADMITTED',
                    reserved_until = null,
                    confirmed_at = current_timestamp(3),
                    version = version + 1,
                    updated_by = :userId,
                    updated_at = current_timestamp(3)
                where id = :id
                  and tenant_id = :tenantId
                  and status = 'PENDING_CONFIRMATION'
                  and version = :version
                """, new MapSqlParameterSource()
                .addValue("id", admission.id())
                .addValue("tenantId", admission.tenantId())
                .addValue("userId", userId)
                .addValue("version", admission.version()));
        return updated == 1;
    }

    @Override
    public boolean discharge(Admission admission, Long userId, String dischargeReason) {
        int updated = jdbcTemplate.update("""
                update care_admission
                set status = 'DISCHARGED',
                    discharged_at = current_timestamp(3),
                    discharged_by = :userId,
                    discharge_reason = :dischargeReason,
                    version = version + 1,
                    updated_by = :userId,
                    updated_at = current_timestamp(3)
                where id = :id
                  and tenant_id = :tenantId
                  and status = 'ADMITTED'
                  and version = :version
                """, new MapSqlParameterSource()
                .addValue("id", admission.id())
                .addValue("tenantId", admission.tenantId())
                .addValue("userId", userId)
                .addValue("dischargeReason", dischargeReason)
                .addValue("version", admission.version()));
        return updated == 1;
    }

    @Override
    public boolean transferBed(Admission admission, TransferBedRequest request, Long userId) {
        int updated = jdbcTemplate.update("""
                update care_admission set bed_id = :targetBedId, version = version + 1,
                    updated_by = :userId, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and status = 'ADMITTED' and version = :version
                """, new MapSqlParameterSource().addValue("id", admission.id()).addValue("tenantId", admission.tenantId())
                .addValue("targetBedId", request.targetBedId()).addValue("userId", userId).addValue("version", admission.version()));
        if (updated != 1) return false;
        jdbcTemplate.update("""
                insert into care_admission_bed_transfer (
                    tenant_id, admission_id, source_bed_id, target_bed_id, reason, transferred_by, transferred_at
                ) values (:tenantId, :admissionId, :sourceBedId, :targetBedId, :reason, :userId, current_timestamp(3))
                """, new MapSqlParameterSource().addValue("tenantId", admission.tenantId()).addValue("admissionId", admission.id())
                .addValue("sourceBedId", admission.bedId()).addValue("targetBedId", request.targetBedId())
                .addValue("reason", request.reason()).addValue("userId", userId));
        return true;
    }

    @Override
    public boolean cancel(Admission admission, Long userId) {
        return jdbcTemplate.update("""
                update care_admission set status = 'CANCELLED', reserved_until = null, version = version + 1, updated_by = :userId, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and status in ('PENDING_ASSESSMENT', 'PENDING_ASSIGNMENT', 'PENDING_CONFIRMATION')
                  and version = :version
                """, new MapSqlParameterSource().addValue("id", admission.id()).addValue("tenantId", admission.tenantId())
                .addValue("userId", userId).addValue("version", admission.version())) == 1;
    }

    @Override
    public List<Admission> findExpiredReservations(OffsetDateTime now) {
        return jdbcTemplate.query("""
                select id, tenant_id, resident_id, bed_id, reserved_until, status, version, applied_at
                from care_admission
                where status = 'PENDING_CONFIRMATION'
                  and reserved_until is not null
                  and reserved_until <= :now
                order by reserved_until, id
                """, new MapSqlParameterSource().addValue("now", java.sql.Timestamp.from(now.toInstant())), rowMapper);
    }

    @Override
    public boolean releaseExpiredReservation(Admission admission, Long userId) {
        return jdbcTemplate.update("""
                update care_admission
                set bed_id = null, reserved_until = null, status = 'PENDING_ASSIGNMENT', version = version + 1,
                    updated_by = :userId, updated_at = current_timestamp(3)
                where id = :id and tenant_id = :tenantId and status = 'PENDING_CONFIRMATION'
                  and reserved_until <= current_timestamp(3) and version = :version
                """, new MapSqlParameterSource().addValue("id", admission.id()).addValue("tenantId", admission.tenantId())
                .addValue("userId", userId).addValue("version", admission.version())) == 1;
    }

    @Override
    public List<AdmissionAuditEvent> findAuditEvents(Long tenantId, Long admissionId) {
        return jdbcTemplate.query("""
                select action, actor_id, related_bed_id, created_at
                from care_audit_log
                where tenant_id = :tenantId and target_type = 'ADMISSION' and target_id = :admissionId
                order by created_at desc, id desc limit 50
                """, new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("admissionId", admissionId),
                (resultSet, rowNum) -> new AdmissionAuditEvent(resultSet.getString("action"),
                        resultSet.getLong("actor_id"), resultSet.getObject("related_bed_id", Long.class),
                        resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC)));
    }
}
