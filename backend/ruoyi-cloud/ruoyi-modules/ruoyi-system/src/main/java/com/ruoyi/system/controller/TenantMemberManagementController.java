package com.ruoyi.system.controller;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.system.service.TenantMemberDirectoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Tenant-scoped member operations. The tenant is accepted only from a Gateway-injected header. */
@RestController
@RequestMapping("/tenant-members")
public class TenantMemberManagementController extends BaseController
{
    private static final String PLATFORM_TENANT_HEADER = "X-Platform-Tenant-Id";
    private static final String INTERNAL_KEY_HEADER = "X-Platform-Internal-Key";

    private final TenantMemberDirectoryService service;
    private final String internalAuthKey;

    public TenantMemberManagementController(TenantMemberDirectoryService service,
                                            @Value("${platform.internal-auth-key:}") String internalAuthKey)
    {
        this.service = service;
        this.internalAuthKey = internalAuthKey;
    }

    @RequiresPermissions("system:user:list")
    @GetMapping
    public AjaxResult list(@RequestHeader(value = PLATFORM_TENANT_HEADER, required = false) String tenantId,
                           @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String suppliedKey)
    {
        return success(service.list(currentTenantId(tenantId, suppliedKey)));
    }

    @RequiresPermissions("system:user:edit")
    @Log(title = "Tenant member management", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestHeader(value = PLATFORM_TENANT_HEADER, required = false) String tenantId,
                          @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String suppliedKey,
                          @Valid @RequestBody TenantMemberRequest request)
    {
        service.addOrReactivate(currentTenantId(tenantId, suppliedKey), request.userId(), request.roleIds());
        return success();
    }

    @RequiresPermissions("system:user:edit")
    @Log(title = "Tenant member roles", businessType = BusinessType.GRANT)
    @PutMapping("/{userId}/roles")
    public AjaxResult replaceRoles(@PathVariable Long userId,
                                   @RequestHeader(value = PLATFORM_TENANT_HEADER, required = false) String tenantId,
                                   @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String suppliedKey,
                                   @Valid @RequestBody TenantMemberRolesRequest request)
    {
        service.replaceRoles(currentTenantId(tenantId, suppliedKey), userId, request.roleIds());
        return success();
    }

    @RequiresPermissions("system:user:edit")
    @Log(title = "Tenant member status", businessType = BusinessType.UPDATE)
    @PutMapping("/{userId}/status")
    public AjaxResult changeStatus(@PathVariable Long userId,
                                   @RequestHeader(value = PLATFORM_TENANT_HEADER, required = false) String tenantId,
                                   @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String suppliedKey,
                                   @Valid @RequestBody TenantMemberStatusRequest request)
    {
        service.changeStatus(currentTenantId(tenantId, suppliedKey), userId, request.status());
        return success();
    }

    private Long currentTenantId(String tenantId, String suppliedKey)
    {
        if (!validInternalKey(suppliedKey))
        {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Trusted Gateway context is required");
        }
        if (!StringUtils.hasText(tenantId))
        {
            throw new ServiceException("Trusted tenant context is invalid");
        }
        try
        {
            return Long.valueOf(tenantId);
        }
        catch (NumberFormatException exception)
        {
            throw new ServiceException("Trusted tenant context is invalid");
        }
    }

    private boolean validInternalKey(String suppliedKey)
    {
        return StringUtils.hasText(internalAuthKey) && StringUtils.hasText(suppliedKey)
                && MessageDigest.isEqual(internalAuthKey.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8));
    }

    public record TenantMemberRequest(@NotNull Long userId, @NotEmpty List<Long> roleIds)
    {
    }

    public record TenantMemberRolesRequest(@NotEmpty List<Long> roleIds)
    {
    }

    public record TenantMemberStatusRequest(@NotNull @Pattern(regexp = "[01]") String status)
    {
    }
}
