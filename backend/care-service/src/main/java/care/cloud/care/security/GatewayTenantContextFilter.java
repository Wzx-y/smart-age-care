package care.cloud.care.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RuoYi Gateway must validate the access token before forwarding these internal headers.
 * Direct service exposure is prohibited; production deployment additionally restricts this
 * service to the internal network and replaces header forwarding with signed internal claims.
 */
@Component
public class GatewayTenantContextFilter extends OncePerRequestFilter {
    private static final String USER_HEADER = "X-Platform-User-Id";
    private static final String TENANT_HEADER = "X-Platform-Tenant-Id";
    private static final String INTERNAL_KEY_HEADER = "X-Platform-Internal-Key";
    private final String internalAuthKey;

    public GatewayTenantContextFilter(@Value("${platform.internal-auth-key:}") String internalAuthKey) {
        this.internalAuthKey = internalAuthKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            if (!validInternalKey(request.getHeader(INTERNAL_KEY_HEADER))) {
                throw new IllegalArgumentException("Invalid gateway credential");
            }
            TenantContext.set(new TenantPrincipal(
                    Long.valueOf(request.getHeader(USER_HEADER)),
                    Long.valueOf(request.getHeader(TENANT_HEADER))
            ));
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"缺少有效的平台身份上下文\",\"data\":null,\"traceId\":null}");
        } finally {
            TenantContext.clear();
        }
    }

    private boolean validInternalKey(String suppliedKey) {
        return StringUtils.hasText(internalAuthKey) && StringUtils.hasText(suppliedKey)
                && MessageDigest.isEqual(internalAuthKey.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8));
    }
}
