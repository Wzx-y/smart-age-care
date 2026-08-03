package care.cloud.care.security;

public class CareAccessDeniedException extends RuntimeException {
    public CareAccessDeniedException() { super("当前成员没有该养老业务操作权限"); }
}
