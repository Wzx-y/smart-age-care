package care.cloud.care.security;

import java.util.Set;

public record TenantMember(Long tenantId, Long userId, boolean active, Set<String> permissions) {
}
