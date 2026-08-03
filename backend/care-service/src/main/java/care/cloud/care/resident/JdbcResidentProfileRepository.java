package care.cloud.care.resident;

import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcResidentProfileRepository implements ResidentProfileRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<ResidentContact> contactMapper = (resultSet, rowNum) -> new ResidentContact(
            resultSet.getLong("id"), resultSet.getLong("resident_id"), resultSet.getString("name"),
            resultSet.getString("relationship_text"), resultSet.getString("phone"), resultSet.getBoolean("is_primary"),
            resultSet.getLong("version"));
    private final RowMapper<ResidentAssessment> assessmentMapper = (resultSet, rowNum) -> new ResidentAssessment(
            resultSet.getLong("id"), resultSet.getLong("resident_id"), resultSet.getString("assessment_type"),
            resultSet.getObject("assessment_date", java.time.LocalDate.class), resultSet.getObject("score", Integer.class),
            resultSet.getString("risk_level"), resultSet.getString("note"), resultSet.getLong("assessor_id"),
            resultSet.getLong("version"),
            resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC));
    private final RowMapper<ResidentAttachment> attachmentMapper = (resultSet, rowNum) -> new ResidentAttachment(
            resultSet.getLong("id"), resultSet.getLong("resident_id"), resultSet.getString("file_name"),
            resultSet.getString("content_type"), resultSet.getLong("byte_size"), resultSet.getString("storage_key"),
            resultSet.getString("upload_status"), resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
            resultSet.getTimestamp("uploaded_at") == null ? null : resultSet.getTimestamp("uploaded_at").toInstant().atOffset(ZoneOffset.UTC));
    private final RowMapper<ResidentHealthProfile> healthProfileMapper = (resultSet, rowNum) -> new ResidentHealthProfile(
            resultSet.getLong("resident_id"), resultSet.getString("blood_type"), resultSet.getString("allergy_summary"),
            resultSet.getString("chronic_conditions"), resultSet.getString("medication_notes"), resultSet.getString("care_level"),
            resultSet.getString("mobility_status"), resultSet.getString("cognition_status"), resultSet.getString("nutrition_risk"),
            resultSet.getString("fall_risk"), resultSet.getString("pressure_injury_risk"), resultSet.getString("infection_risk"),
            resultSet.getString("care_notes"), resultSet.getLong("updated_by"), resultSet.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.UTC));

    public JdbcResidentProfileRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public java.util.Optional<ResidentHealthProfile> findHealthProfile(Long tenantId, Long residentId) {
        return jdbcTemplate.query("""
                select resident_id, blood_type, allergy_summary, chronic_conditions, medication_notes, care_level,
                       mobility_status, cognition_status, nutrition_risk, fall_risk, pressure_injury_risk,
                       infection_risk, care_notes, updated_by, updated_at
                from care_resident_health_profile where tenant_id = :tenantId and resident_id = :residentId
                """, residentParameters(tenantId, residentId), healthProfileMapper).stream().findFirst();
    }

    @Override
    public ResidentHealthProfile saveHealthProfile(Long tenantId, Long residentId, Long userId, UpdateResidentHealthProfileRequest request) {
        MapSqlParameterSource parameters = residentParameters(tenantId, residentId).addValue("userId", userId)
                .addValue("bloodType", request.bloodType()).addValue("allergySummary", request.allergySummary())
                .addValue("chronicConditions", request.chronicConditions()).addValue("medicationNotes", request.medicationNotes())
                .addValue("careLevel", request.careLevel()).addValue("mobilityStatus", request.mobilityStatus())
                .addValue("cognitionStatus", request.cognitionStatus()).addValue("nutritionRisk", request.nutritionRisk())
                .addValue("fallRisk", request.fallRisk()).addValue("pressureInjuryRisk", request.pressureInjuryRisk())
                .addValue("infectionRisk", request.infectionRisk()).addValue("careNotes", request.careNotes());
        jdbcTemplate.update("""
                insert into care_resident_health_profile (resident_id, tenant_id, blood_type, allergy_summary, chronic_conditions,
                    medication_notes, care_level, mobility_status, cognition_status, nutrition_risk, fall_risk,
                    pressure_injury_risk, infection_risk, care_notes, updated_by, created_at, updated_at)
                values (:residentId, :tenantId, :bloodType, :allergySummary, :chronicConditions, :medicationNotes, :careLevel,
                    :mobilityStatus, :cognitionStatus, :nutritionRisk, :fallRisk, :pressureInjuryRisk, :infectionRisk,
                    :careNotes, :userId, current_timestamp(3), current_timestamp(3))
                on duplicate key update blood_type = values(blood_type), allergy_summary = values(allergy_summary),
                    chronic_conditions = values(chronic_conditions), medication_notes = values(medication_notes), care_level = values(care_level),
                    mobility_status = values(mobility_status), cognition_status = values(cognition_status), nutrition_risk = values(nutrition_risk),
                    fall_risk = values(fall_risk), pressure_injury_risk = values(pressure_injury_risk), infection_risk = values(infection_risk),
                    care_notes = values(care_notes), updated_by = values(updated_by), updated_at = current_timestamp(3)
                """, parameters);
        return findHealthProfile(tenantId, residentId).orElseThrow();
    }

    @Override
    public List<ResidentContact> findContacts(Long tenantId, Long residentId) {
        return jdbcTemplate.query("""
                select id, resident_id, name, relationship_text, phone, is_primary, version
                from care_resident_contact
                where tenant_id = :tenantId and resident_id = :residentId
                order by is_primary desc, id desc
                """, residentParameters(tenantId, residentId), contactMapper);
    }

    @Override
    public void clearPrimaryContact(Long tenantId, Long residentId) {
        jdbcTemplate.update("""
                update care_resident_contact set is_primary = false
                where tenant_id = :tenantId and resident_id = :residentId and is_primary = true
                """, residentParameters(tenantId, residentId));
    }

    @Override
    public void clearPrimaryContactExcept(Long tenantId, Long residentId, Long contactId) {
        jdbcTemplate.update("""
                update care_resident_contact set is_primary = false, version = version + 1, updated_at = current_timestamp(3)
                where tenant_id = :tenantId and resident_id = :residentId and id <> :contactId and is_primary = true
                """, residentParameters(tenantId, residentId).addValue("contactId", contactId));
    }

    @Override
    public ResidentContact saveContact(Long tenantId, Long residentId, CreateResidentContactRequest request) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into care_resident_contact (tenant_id, resident_id, name, relationship_text, phone, is_primary, version, created_at, updated_at)
                values (:tenantId, :residentId, :name, :relationshipText, :phone, :primaryContact, 1, current_timestamp(3), current_timestamp(3))
                """, residentParameters(tenantId, residentId)
                .addValue("name", request.name()).addValue("relationshipText", request.relationshipText())
                .addValue("phone", request.phone()).addValue("primaryContact", request.primaryContact()), keyHolder, new String[]{"id"});
        Number id = keyHolder.getKey();
        if (id == null) throw new IllegalStateException("创建联系人后未返回主键");
        return jdbcTemplate.queryForObject("""
                select id, resident_id, name, relationship_text, phone, is_primary, version
                from care_resident_contact where id = :id and tenant_id = :tenantId
                """, new MapSqlParameterSource().addValue("id", id.longValue()).addValue("tenantId", tenantId), contactMapper);
    }

    @Override
    public boolean updateContact(Long tenantId, Long residentId, Long contactId, UpdateResidentContactRequest request) {
        return jdbcTemplate.update("""
                update care_resident_contact
                set name = :name, relationship_text = :relationshipText, phone = :phone, is_primary = :primaryContact,
                    version = version + 1, updated_at = current_timestamp(3)
                where id = :contactId and tenant_id = :tenantId and resident_id = :residentId and version = :version
                """, residentParameters(tenantId, residentId).addValue("contactId", contactId).addValue("version", request.version())
                .addValue("name", request.name()).addValue("relationshipText", request.relationshipText())
                .addValue("phone", request.phone()).addValue("primaryContact", request.primaryContact())) == 1;
    }

    @Override
    public boolean deleteContact(Long tenantId, Long residentId, Long contactId, long version) {
        return jdbcTemplate.update("""
                delete from care_resident_contact
                where id = :contactId and tenant_id = :tenantId and resident_id = :residentId and version = :version
                """, residentParameters(tenantId, residentId).addValue("contactId", contactId).addValue("version", version)) == 1;
    }

    @Override
    public List<ResidentAssessment> findAssessments(Long tenantId, Long residentId) {
        return jdbcTemplate.query("""
                select id, resident_id, assessment_type, assessment_date, score, risk_level, note, assessor_id, version, created_at
                from care_resident_assessment
                where tenant_id = :tenantId and resident_id = :residentId
                order by assessment_date desc, id desc
                """, residentParameters(tenantId, residentId), assessmentMapper);
    }

    @Override
    public ResidentAssessment saveAssessment(Long tenantId, Long residentId, Long assessorId, CreateResidentAssessmentRequest request) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into care_resident_assessment (tenant_id, resident_id, assessment_type, assessment_date, score, risk_level, note, assessor_id, created_at)
                values (:tenantId, :residentId, :assessmentType, :assessmentDate, :score, :riskLevel, :note, :assessorId, current_timestamp(3))
                """, residentParameters(tenantId, residentId).addValue("assessmentType", request.assessmentType())
                .addValue("assessmentDate", request.assessmentDate()).addValue("score", request.score())
                .addValue("riskLevel", request.riskLevel()).addValue("note", request.note()).addValue("assessorId", assessorId), keyHolder, new String[]{"id"});
        Number id = keyHolder.getKey();
        if (id == null) throw new IllegalStateException("创建评估后未返回主键");
        return jdbcTemplate.queryForObject("""
                select id, resident_id, assessment_type, assessment_date, score, risk_level, note, assessor_id, version, created_at
                from care_resident_assessment where id = :id and tenant_id = :tenantId
                """, new MapSqlParameterSource().addValue("id", id.longValue()).addValue("tenantId", tenantId), assessmentMapper);
    }

    @Override
    public boolean updateAssessment(Long tenantId, Long residentId, Long assessmentId, UpdateResidentAssessmentRequest request) {
        return jdbcTemplate.update("""
                update care_resident_assessment
                set assessment_type = :assessmentType, assessment_date = :assessmentDate, score = :score,
                    risk_level = :riskLevel, note = :note, version = version + 1
                where id = :assessmentId and tenant_id = :tenantId and resident_id = :residentId and version = :version
                """, residentParameters(tenantId, residentId).addValue("assessmentId", assessmentId).addValue("version", request.version())
                .addValue("assessmentType", request.assessmentType()).addValue("assessmentDate", request.assessmentDate())
                .addValue("score", request.score()).addValue("riskLevel", request.riskLevel()).addValue("note", request.note())) == 1;
    }

    @Override
    public boolean isAssessmentReferenced(Long tenantId, Long residentId, Long assessmentId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from care_plan
                where tenant_id = :tenantId and resident_id = :residentId and assessment_id = :assessmentId
                  and status <> 'CANCELLED'
                """, residentParameters(tenantId, residentId).addValue("assessmentId", assessmentId), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean deleteAssessment(Long tenantId, Long residentId, Long assessmentId, long version) {
        return jdbcTemplate.update("""
                delete from care_resident_assessment
                where id = :assessmentId and tenant_id = :tenantId and resident_id = :residentId and version = :version
                """, residentParameters(tenantId, residentId).addValue("assessmentId", assessmentId).addValue("version", version)) == 1;
    }

    @Override
    public List<ResidentAttachment> findAttachments(Long tenantId, Long residentId) {
        return jdbcTemplate.query("""
                select id, resident_id, file_name, content_type, byte_size, storage_key, upload_status, created_at, uploaded_at
                from care_resident_attachment
                where tenant_id = :tenantId and resident_id = :residentId
                order by id desc
                """, residentParameters(tenantId, residentId), attachmentMapper);
    }

    @Override
    public java.util.Optional<ResidentAttachment> findAttachmentById(Long tenantId, Long residentId, Long attachmentId) {
        return jdbcTemplate.query("""
                select id, resident_id, file_name, content_type, byte_size, storage_key, upload_status, created_at, uploaded_at
                from care_resident_attachment
                where id = :attachmentId and tenant_id = :tenantId and resident_id = :residentId
                """, residentParameters(tenantId, residentId).addValue("attachmentId", attachmentId), attachmentMapper).stream().findFirst();
    }

    @Override
    public ResidentAttachment saveAttachment(Long tenantId, Long residentId, Long userId, CreateResidentAttachmentRequest request, String storageKey) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
                insert into care_resident_attachment (tenant_id, resident_id, file_name, content_type, byte_size, storage_key, upload_status, created_by, created_at)
                values (:tenantId, :residentId, :fileName, :contentType, :byteSize, :storageKey, 'PENDING_UPLOAD', :userId, current_timestamp(3))
                """, residentParameters(tenantId, residentId).addValue("fileName", request.fileName())
                .addValue("contentType", request.contentType()).addValue("byteSize", request.byteSize())
                .addValue("storageKey", storageKey).addValue("userId", userId), keyHolder, new String[]{"id"});
        Number id = keyHolder.getKey();
        if (id == null) throw new IllegalStateException("创建附件记录后未返回主键");
        return jdbcTemplate.queryForObject("""
                select id, resident_id, file_name, content_type, byte_size, storage_key, upload_status, created_at, uploaded_at
                from care_resident_attachment where id = :id and tenant_id = :tenantId
                """, new MapSqlParameterSource().addValue("id", id.longValue()).addValue("tenantId", tenantId), attachmentMapper);
    }

    @Override
    public boolean markAttachmentUploaded(Long tenantId, Long residentId, Long attachmentId) {
        return jdbcTemplate.update("""
                update care_resident_attachment
                set upload_status = 'UPLOADED', uploaded_at = current_timestamp(3)
                where id = :attachmentId
                  and tenant_id = :tenantId
                  and resident_id = :residentId
                  and upload_status = 'PENDING_UPLOAD'
                """, residentParameters(tenantId, residentId).addValue("attachmentId", attachmentId)) == 1;
    }

    private MapSqlParameterSource residentParameters(Long tenantId, Long residentId) {
        return new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("residentId", residentId);
    }
}
