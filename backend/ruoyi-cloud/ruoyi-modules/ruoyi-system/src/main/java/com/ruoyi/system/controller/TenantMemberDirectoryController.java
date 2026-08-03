package com.ruoyi.system.controller;

import com.ruoyi.system.service.TenantMemberDirectoryService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/v1/tenant-members")
public class TenantMemberDirectoryController
{
    private static final String INTERNAL_KEY_HEADER = "X-Platform-Internal-Key";

    private final TenantMemberDirectoryService service;
    private final String internalAuthKey;

    public TenantMemberDirectoryController(TenantMemberDirectoryService service,
                                           @Value("${platform.internal-auth-key:}") String internalAuthKey)
    {
        this.service = service;
        this.internalAuthKey = internalAuthKey;
    }

    @GetMapping("/{userId}")
    public TenantMemberDirectoryService.TenantMemberDirectoryResult find(
            @PathVariable Long userId,
            @RequestParam Long tenantId,
            @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String suppliedKey)
    {
        if (!validInternalKey(suppliedKey))
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal platform credential rejected");
        }
        return service.find(tenantId, userId);
    }

    private boolean validInternalKey(String suppliedKey)
    {
        if (!StringUtils.hasText(internalAuthKey) || !StringUtils.hasText(suppliedKey))
        {
            return false;
        }
        return MessageDigest.isEqual(internalAuthKey.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8));
    }
}
