package care.cloud.care.security;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class RuoYiTenantMemberDirectory implements TenantMemberDirectory {
    private final String memberDirectoryUrl;
    private final String internalAuthKey;
    private final RestClient restClient;

    public RuoYiTenantMemberDirectory(@Value("${platform.member-directory-url:}") String memberDirectoryUrl,
                                      @Value("${platform.internal-auth-key:}") String internalAuthKey) {
        this.memberDirectoryUrl = memberDirectoryUrl;
        this.internalAuthKey = internalAuthKey;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public TenantMember requireActiveMember(Long tenantId, Long userId) {
        if (!StringUtils.hasText(memberDirectoryUrl) || !StringUtils.hasText(internalAuthKey)) throw new MemberDirectoryUnavailableException();
        MemberResponse response;
        try {
            response = restClient.get().uri(memberDirectoryUrl + "/internal/v1/tenant-members/{userId}?tenantId={tenantId}", userId, tenantId)
                    .header("X-Platform-Internal-Key", internalAuthKey)
                    .retrieve().body(MemberResponse.class);
        } catch (RuntimeException exception) {
            throw new MemberDirectoryUnavailableException();
        }
        if (response == null || !response.active() || response.tenantId() == null || response.userId() == null
                || !tenantId.equals(response.tenantId()) || !userId.equals(response.userId())) throw new CareAccessDeniedException();
        return new TenantMember(response.tenantId(), response.userId(), true, response.permissions() == null ? Set.of() : Set.copyOf(response.permissions()));
    }

    private record MemberResponse(Long tenantId, Long userId, boolean active, Set<String> permissions) { }
}
