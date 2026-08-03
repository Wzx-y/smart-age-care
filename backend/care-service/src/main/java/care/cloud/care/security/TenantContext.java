package care.cloud.care.security;

public final class TenantContext {
    private static final ThreadLocal<TenantPrincipal> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantPrincipal principal) {
        CURRENT.set(principal);
    }

    public static TenantPrincipal requireCurrent() {
        TenantPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new TenantContextMissingException();
        }
        return principal;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
