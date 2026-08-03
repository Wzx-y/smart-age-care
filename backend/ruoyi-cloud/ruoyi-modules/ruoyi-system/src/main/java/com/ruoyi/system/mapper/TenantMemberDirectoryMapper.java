package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TenantMemberDirectoryMapper
{
    @Select("""
            select count(1)
            from care_tenant_member member
            join sys_user user on user.user_id = member.user_id
            where member.tenant_id = #{tenantId} and member.user_id = #{userId}
              and member.status = '0' and user.status = '0' and user.del_flag = '0'
            """)
    int countActiveMembership(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Select("""
            select distinct menu.perms
            from care_tenant_member_role member_role
            join sys_role role on role.role_id = member_role.role_id
            join sys_role_menu role_menu on role_menu.role_id = role.role_id
            join sys_menu menu on menu.menu_id = role_menu.menu_id
            where member_role.tenant_id = #{tenantId} and member_role.user_id = #{userId}
              and role.status = '0' and role.del_flag = '0'
              and menu.status = '0' and menu.perms is not null and menu.perms <> ''
            """)
    List<String> selectPermissionCodes(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Select("""
            select member.tenant_id as tenantId, user.user_id as userId, user.user_name as userName,
                   user.nick_name as nickName, user.email as email, user.phonenumber as phoneNumber,
                   member.status as membershipStatus, user.status as userStatus, dept.dept_name as deptName,
                   coalesce(group_concat(distinct role.role_id order by role.role_id separator ','), '') as roleIds,
                   coalesce(group_concat(distinct role.role_name order by role.role_id separator ', '), '') as roleNames,
                   coalesce(group_concat(distinct role.data_scope order by role.role_id separator ','), '') as dataScopes
            from care_tenant_member member
            join sys_user user on user.user_id = member.user_id and user.del_flag = '0'
            left join sys_dept dept on dept.dept_id = user.dept_id
            left join care_tenant_member_role member_role
              on member_role.tenant_id = member.tenant_id and member_role.user_id = member.user_id
            left join sys_role role on role.role_id = member_role.role_id and role.del_flag = '0'
            where member.tenant_id = #{tenantId}
            group by member.tenant_id, user.user_id, user.user_name, user.nick_name, user.email,
                     user.phonenumber, member.status, user.status, dept.dept_name
            order by user.user_id
            """)
    List<TenantMemberSummary> selectMembers(@Param("tenantId") Long tenantId);

    @Select("""
            select count(1) from sys_user
            where user_id = #{userId} and del_flag = '0'
            """)
    int countAvailableUser(@Param("userId") Long userId);

    @Select("select count(1) from care_tenant_member where tenant_id = #{tenantId} and user_id = #{userId}")
    int countMembership(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Select("select role_id from sys_role where status = '0' and del_flag = '0'")
    List<Long> selectActiveRoleIds();

    @Insert("""
            insert into care_tenant_member (tenant_id, user_id, status)
            values (#{tenantId}, #{userId}, #{status})
            on duplicate key update status = values(status), updated_at = current_timestamp
            """)
    int upsertMembership(@Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("status") String status);

    @Update("""
            update care_tenant_member
            set status = #{status}, updated_at = current_timestamp
            where tenant_id = #{tenantId} and user_id = #{userId}
            """)
    int updateMembershipStatus(@Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("status") String status);

    @Delete("delete from care_tenant_member_role where tenant_id = #{tenantId} and user_id = #{userId}")
    int deleteRoleAssignments(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Insert("""
            <script>
            insert into care_tenant_member_role (tenant_id, user_id, role_id) values
            <foreach collection="roleIds" item="roleId" separator=",">
              (#{tenantId}, #{userId}, #{roleId})
            </foreach>
            </script>
            """)
    int insertRoleAssignments(@Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    record TenantMemberSummary(Long tenantId, Long userId, String userName, String nickName, String email,
                               String phoneNumber, String membershipStatus, String userStatus, String deptName,
                               String roleIds, String roleNames, String dataScopes)
    {
    }
}
