package com.ruoyi.gateway.filter;

import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.utils.ServletUtils;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Converts a browser-selected tenant into trusted headers only after the RuoYi System
 * member directory confirms that the authenticated user is active in that tenant.
 */
@Component
public class TenantContextFilter implements GlobalFilter, Ordered
{
    private static final String CARE_PATH_PREFIX = "/care/";
    private static final String TENANT_MEMBER_MANAGEMENT_PATH_PREFIX = "/system/tenant-members";
    private static final String REQUEST_TENANT_HEADER = "X-Tenant-Id";
    private static final String PLATFORM_USER_HEADER = "X-Platform-User-Id";
    private static final String PLATFORM_TENANT_HEADER = "X-Platform-Tenant-Id";
    private static final String INTERNAL_KEY_HEADER = "X-Platform-Internal-Key";
    private static final Set<String> UNTRUSTED_HEADERS = Set.of(
            REQUEST_TENANT_HEADER, PLATFORM_USER_HEADER, PLATFORM_TENANT_HEADER, INTERNAL_KEY_HEADER);

    private final WebClient webClient;
    private final String memberDirectoryUrl;
    private final String internalAuthKey;

    public TenantContextFilter(WebClient.Builder webClientBuilder,
                               @Value("${platform.member-directory-url:http://ruoyi-system:9201}") String memberDirectoryUrl,
                               @Value("${platform.internal-auth-key:}") String internalAuthKey)
    {
        this.webClient = webClientBuilder.build();
        this.memberDirectoryUrl = memberDirectoryUrl;
        this.internalAuthKey = internalAuthKey;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        if (!requiresTenantContext(request.getURI().getPath()))
        {
            return chain.filter(exchange.mutate().request(removeUntrustedHeaders(request).build()).build());
        }

        String selectedTenantId = request.getHeaders().getFirst(REQUEST_TENANT_HEADER);
        String encodedUserId = request.getHeaders().getFirst(SecurityConstants.DETAILS_USER_ID);
        String userId = encodedUserId == null ? null : ServletUtils.urlDecode(encodedUserId);
        if (!StringUtils.hasText(selectedTenantId) || !StringUtils.hasText(userId))
        {
            return response(exchange, HttpStatus.BAD_REQUEST, "A selected tenant is required for care requests", 400);
        }
        Long tenantId;
        Long currentUserId;
        try
        {
            tenantId = Long.valueOf(selectedTenantId);
            currentUserId = Long.valueOf(userId);
        }
        catch (NumberFormatException exception)
        {
            return response(exchange, HttpStatus.BAD_REQUEST, "Invalid tenant context", 400);
        }
        if (!StringUtils.hasText(memberDirectoryUrl) || !StringUtils.hasText(internalAuthKey))
        {
            return response(exchange, HttpStatus.SERVICE_UNAVAILABLE, "Tenant directory is unavailable", 503);
        }

        return webClient.get()
                .uri(memberDirectoryUrl + "/internal/v1/tenant-members/{userId}?tenantId={tenantId}", currentUserId, tenantId)
                .header(INTERNAL_KEY_HEADER, internalAuthKey)
                .retrieve()
                .bodyToMono(TenantMemberResponse.class)
                .flatMap(member -> forwardOrReject(exchange, chain, request, currentUserId, tenantId, member))
                .onErrorResume(exception -> response(exchange, HttpStatus.SERVICE_UNAVAILABLE, "Tenant directory is unavailable", 503));
    }

    private boolean requiresTenantContext(String path)
    {
        return path.startsWith(CARE_PATH_PREFIX) || path.startsWith(TENANT_MEMBER_MANAGEMENT_PATH_PREFIX);
    }

    private Mono<Void> forwardOrReject(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpRequest request,
                                       Long userId, Long tenantId, TenantMemberResponse member)
    {
        if (member == null || !member.active() || !tenantId.equals(member.tenantId()) || !userId.equals(member.userId()))
        {
            return response(exchange, HttpStatus.FORBIDDEN, "Tenant membership is required", 403);
        }
        ServerHttpRequest.Builder mutated = removeUntrustedHeaders(request);
        mutated.header(PLATFORM_USER_HEADER, userId.toString());
        mutated.header(PLATFORM_TENANT_HEADER, tenantId.toString());
        mutated.header(INTERNAL_KEY_HEADER, internalAuthKey);
        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    private ServerHttpRequest.Builder removeUntrustedHeaders(ServerHttpRequest request)
    {
        return request.mutate().headers(headers -> UNTRUSTED_HEADERS.forEach(headers::remove));
    }

    private Mono<Void> response(ServerWebExchange exchange, HttpStatus status, String message, int code)
    {
        return ServletUtils.webFluxResponseWriter(exchange.getResponse(), status, message, code);
    }

    @Override
    public int getOrder()
    {
        return -190;
    }

    private record TenantMemberResponse(Long tenantId, Long userId, boolean active)
    {
    }
}
