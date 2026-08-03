package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.ruoyi.system.controller.TenantDirectoryController.TenantDirectoryRequest;

@org.apache.ibatis.annotations.Mapper
public interface TenantDirectoryMapper {
    @Select("select d.tenant_id as tenantId, d.tenant_code as code, d.tenant_name as name, d.plan_code as plan, d.region, d.status, d.version, (select count(1) from care_tenant_member m where m.tenant_id = d.tenant_id and m.status = '0') as members from care_tenant_directory d join care_tenant_member m on m.tenant_id = d.tenant_id and m.user_id = #{userId} and m.status = '0' where d.status = '0' order by d.tenant_id")
    List<TenantDirectorySummary> selectForUser(@Param("userId") Long userId);

    @Insert("insert into care_tenant_directory (tenant_code, tenant_name, plan_code, region, status, version, created_at, updated_at) values (#{request.code}, #{request.name}, #{request.plan}, #{request.region}, '0', 0, current_timestamp(3), current_timestamp(3))")
    int insert(@Param("request") TenantDirectoryRequest request);

    @Select("select last_insert_id()")
    Long lastInsertId();

    @Insert("insert into care_tenant_member (tenant_id, user_id, status) values (#{tenantId}, #{userId}, '0')")
    int addOwner(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Update("update care_tenant_directory set tenant_name = #{request.name}, plan_code = #{request.plan}, region = #{request.region}, version = version + 1, updated_at = current_timestamp(3) where tenant_id = #{tenantId} and version = #{version}")
    int update(@Param("tenantId") Long tenantId, @Param("version") Long version, @Param("request") TenantDirectoryRequest request);

    @Update("update care_tenant_directory set status = #{status}, version = version + 1, updated_at = current_timestamp(3) where tenant_id = #{tenantId} and version = #{version}")
    int updateStatus(@Param("tenantId") Long tenantId, @Param("version") Long version, @Param("status") String status);

    record TenantDirectorySummary(Long tenantId, String code, String name, String plan, String region, String status, Long version, Long members) { }
}
