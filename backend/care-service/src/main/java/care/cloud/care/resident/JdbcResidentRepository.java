package care.cloud.care.resident;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class JdbcResidentRepository implements ResidentRepository {
    private static final String RESIDENT_COLUMNS = """
            id, tenant_id, name, gender, birth_date, status, current_bed_id,
            emergency_contact_name, emergency_contact_phone, created_at, updated_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<Resident> rowMapper = (resultSet, rowNum) -> new Resident(
            resultSet.getLong("id"),
            resultSet.getLong("tenant_id"),
            resultSet.getString("name"),
            resultSet.getString("gender"),
            resultSet.getObject("birth_date", java.time.LocalDate.class),
            ResidentStatus.valueOf(resultSet.getString("status")),
            resultSet.getObject("current_bed_id", Long.class),
            resultSet.getString("emergency_contact_name"),
            resultSet.getString("emergency_contact_phone"),
            resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
            resultSet.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.UTC)
    );

    public JdbcResidentRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Resident save(Resident resident) {
        if (resident.id() != null) {
            throw new IllegalArgumentException("创建长者档案时不能指定记录 ID");
        }
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", resident.tenantId())
                .addValue("name", resident.name())
                .addValue("gender", resident.gender())
                .addValue("birthDate", resident.birthDate())
                .addValue("status", resident.status().name())
                .addValue("emergencyContactName", resident.emergencyContactName())
                .addValue("emergencyContactPhone", resident.emergencyContactPhone())
                .addValue("createdAt", Timestamp.from(resident.createdAt().toInstant()))
                .addValue("updatedAt", Timestamp.from(resident.updatedAt().toInstant()));

        jdbcTemplate.update("""
                insert into care_resident (
                    tenant_id, name, gender, birth_date, status,
                    emergency_contact_name, emergency_contact_phone, created_at, updated_at
                ) values (
                    :tenantId, :name, :gender, :birthDate, :status,
                    :emergencyContactName, :emergencyContactPhone, :createdAt, :updatedAt
                )
                """, parameters, keyHolder, new String[]{"id"});

        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("创建长者档案后未返回主键");
        }
        return findByIdAndTenantId(generatedId.longValue(), resident.tenantId())
                .orElseThrow(() -> new IllegalStateException("创建长者档案后无法读取记录"));
    }

    @Override
    public boolean update(Resident resident) {
        int updated = jdbcTemplate.update("""
                update care_resident
                set name = :name,
                    gender = :gender,
                    birth_date = :birthDate,
                    emergency_contact_name = :emergencyContactName,
                    emergency_contact_phone = :emergencyContactPhone,
                    updated_at = :updatedAt
                where id = :id
                  and tenant_id = :tenantId
                  and archived_at is null
                """, new MapSqlParameterSource()
                .addValue("id", resident.id())
                .addValue("tenantId", resident.tenantId())
                .addValue("name", resident.name())
                .addValue("gender", resident.gender())
                .addValue("birthDate", resident.birthDate())
                .addValue("emergencyContactName", resident.emergencyContactName())
                .addValue("emergencyContactPhone", resident.emergencyContactPhone())
                .addValue("updatedAt", Timestamp.from(resident.updatedAt().toInstant())));
        return updated == 1;
    }

    @Override
    public boolean archive(Long id, Long tenantId, java.time.OffsetDateTime archivedAt) {
        int updated = jdbcTemplate.update("""
                update care_resident
                set archived_at = :archivedAt,
                    updated_at = :archivedAt
                where id = :id
                  and tenant_id = :tenantId
                  and archived_at is null
                  and status in ('DISCHARGED', 'DECEASED')
                  and current_bed_id is null
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("tenantId", tenantId)
                .addValue("archivedAt", Timestamp.from(archivedAt.toInstant())));
        return updated == 1;
    }

    @Override
    public Optional<Resident> findByIdAndTenantId(Long id, Long tenantId) {
        List<Resident> residents = jdbcTemplate.query("""
                select %s from care_resident
                where id = :id and tenant_id = :tenantId and archived_at is null
                """.formatted(RESIDENT_COLUMNS),
                new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId),
                rowMapper);
        return residents.stream().findFirst();
    }

    @Override
    public List<Resident> findByTenantId(Long tenantId, String keyword) {
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("tenantId", tenantId);
        String keywordClause = "";
        if (StringUtils.hasText(keyword)) {
            keywordClause = " and lower(name) like lower(:keyword)";
            parameters.addValue("keyword", "%" + keyword.trim() + "%");
        }
        return jdbcTemplate.query(("""
                select %s from care_resident
                where tenant_id = :tenantId and archived_at is null%s
                order by id desc
                """).formatted(RESIDENT_COLUMNS, keywordClause), parameters, rowMapper);
    }
}
