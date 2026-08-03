package care.cloud.care.security;

public class TenantContextMissingException extends RuntimeException {
    public TenantContextMissingException() {
        super("请求缺少经过认证的租户上下文");
    }
}
