package care.cloud.care.security;

public class MemberDirectoryUnavailableException extends RuntimeException {
    public MemberDirectoryUnavailableException() { super("RuoYi 成员权限服务未配置或暂不可用"); }
}
