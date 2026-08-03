package care.cloud.care.care;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareShiftHandoverRepository implements CareShiftHandoverRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<CareShiftHandover> rowMapper = (resultSet, rowNum) -> new CareShiftHandover(
            resultSet.getLong("id"), resultSet.getLong("tenant_id"), resultSet.getObject("shift_date", java.time.LocalDate.class),
            resultSet.getString("shift_code"), resultSet.getLong("from_user_id"), resultSet.getLong("to_user_id"),
            resultSet.getString("note"), CareShiftHandoverStatus.valueOf(resultSet.getString("status")),
            resultSet.getTimestamp("submitted_at") == null ? null : resultSet.getTimestamp("submitted_at").toInstant().atOffset(ZoneOffset.UTC),
            resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
            resultSet.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.UTC), resultSet.getLong("version"), List.of()
    );

    public JdbcCareShiftHandoverRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CareShiftHandover> findByIdAndTenantId(Long id, Long tenantId) {
        return jdbcTemplate.query(selectSql("where id = :id and tenant_id = :tenantId"),
                new MapSqlParameterSource().addValue("id", id).addValue("tenantId", tenantId), rowMapper).stream().findFirst();
    }

    @Override
    public Optional<CareShiftHandover> findByShiftAndFromUser(Long tenantId, java.time.LocalDate shiftDate, String shiftCode, Long fromUserId) {
        return jdbcTemplate.query(selectSql("where tenant_id = :tenantId and shift_date = :shiftDate and shift_code = :shiftCode and from_user_id = :fromUserId"),
                new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("shiftDate", shiftDate)
                        .addValue("shiftCode", shiftCode).addValue("fromUserId", fromUserId), rowMapper).stream().findFirst();
    }

    @Override
    public List<CareShiftHandover> findByTenantId(Long tenantId) {
        return jdbcTemplate.query(selectSql("where tenant_id = :tenantId order by shift_date desc, created_at desc, id desc"),
                new MapSqlParameterSource().addValue("tenantId", tenantId), rowMapper);
    }

    @Override
    public List<Long> findTaskIds(Long handoverId, Long tenantId) {
        return jdbcTemplate.queryForList("""
                select task_id from care_shift_handover_item
                where handover_id = :handoverId and tenant_id = :tenantId
                order by task_id asc
                """, new MapSqlParameterSource().addValue("handoverId", handoverId).addValue("tenantId", tenantId), Long.class);
    }

    @Override
    public CareShiftHandover create(CareShiftHandover handover) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update("""
                    insert into care_shift_handover (
                        tenant_id, shift_date, shift_code, from_user_id, to_user_id, note, status,
                        created_at, updated_at, version
                    ) values (
                        :tenantId, :shiftDate, :shiftCode, :fromUserId, :toUserId, :note, :status,
                        current_timestamp(3), current_timestamp(3), :version
                    )
                    """, new MapSqlParameterSource().addValue("tenantId", handover.tenantId())
                    .addValue("shiftDate", handover.shiftDate()).addValue("shiftCode", handover.shiftCode())
                    .addValue("fromUserId", handover.fromUserId()).addValue("toUserId", handover.toUserId())
                    .addValue("note", handover.note()).addValue("status", handover.status().name()).addValue("version", handover.version()),
                    keyHolder, new String[]{"id"});
        } catch (DuplicateKeyException exception) {
            throw new CareShiftHandoverConflictException("当前班次已存在交接草稿或已提交交接");
        }
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) throw new IllegalStateException("创建班次交接后未返回主键");
        return findByIdAndTenantId(generatedId.longValue(), handover.tenantId())
                .orElseThrow(() -> new IllegalStateException("创建班次交接后无法读取记录"));
    }

    @Override
    public void createItems(Long tenantId, Long handoverId, List<Long> taskIds) {
        MapSqlParameterSource[] batch = taskIds.stream().map(taskId -> new MapSqlParameterSource()
                .addValue("tenantId", tenantId).addValue("handoverId", handoverId).addValue("taskId", taskId)).toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate("""
                insert into care_shift_handover_item (tenant_id, handover_id, task_id, created_at)
                values (:tenantId, :handoverId, :taskId, current_timestamp(3))
                """, batch);
    }

    @Override
    public boolean submit(CareShiftHandover handover) {
        return jdbcTemplate.update("""
                update care_shift_handover
                set status = 'SUBMITTED', submitted_at = current_timestamp(3), updated_at = current_timestamp(3), version = version + 1
                where id = :id and tenant_id = :tenantId and status = 'DRAFT' and version = :version
                """, new MapSqlParameterSource().addValue("id", handover.id()).addValue("tenantId", handover.tenantId())
                .addValue("version", handover.version())) == 1;
    }

    private String selectSql(String condition) {
        return """
                select id, tenant_id, shift_date, shift_code, from_user_id, to_user_id, note, status, submitted_at,
                       created_at, updated_at, version
                from care_shift_handover %s
                """.formatted(condition);
    }
}
