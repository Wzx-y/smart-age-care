package care.cloud.care.security;

public record TenantPrincipal(Long userId, Long tenantId) {
    public TenantPrincipal {
        if (userId == null || tenantId == null) {
            throw new IllegalArgumentException("用户与租户上下文不能为空");
        }
    }
}
