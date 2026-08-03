package care.cloud.care.security;

public interface TenantMemberDirectory {
    TenantMember requireActiveMember(Long tenantId, Long userId);

    default void requirePermission(Long tenantId, Long userId, String permission) {
        TenantMember member = requireActiveMember(tenantId, userId);
        if (!member.permissions().contains(permission)) throw new CareAccessDeniedException();
    }
}
