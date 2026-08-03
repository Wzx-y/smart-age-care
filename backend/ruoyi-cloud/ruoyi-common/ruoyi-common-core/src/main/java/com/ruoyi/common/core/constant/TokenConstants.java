package com.ruoyi.common.core.constant;

/**
 * Token的Key常量
 * 
 * @author ruoyi
 */
public class TokenConstants
{
    /**
     * 令牌前缀
     */
    public static final String PREFIX = "Bearer ";

    /**
     * 令牌秘钥
     */
    public static final String SECRET = requiredSecret();

    private static String requiredSecret()
    {
        String secret = System.getenv("RUOYI_JWT_SECRET");
        if (secret == null || secret.length() < 64)
        {
            throw new IllegalStateException("RUOYI_JWT_SECRET must be configured with at least 64 characters");
        }
        return secret;
    }

}
