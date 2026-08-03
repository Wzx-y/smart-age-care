package com.ruoyi.system.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.system.mapper.TenantDirectoryMapper;
import com.ruoyi.system.service.TenantDirectoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant-directory")
public class TenantDirectoryController extends BaseController {
    private final TenantDirectoryService service;
    public TenantDirectoryController(TenantDirectoryService service) { this.service = service; }

    @GetMapping
    public AjaxResult list() { return success(service.listMine()); }

    @PostMapping("/{tenantId}/switch")
    public AjaxResult switchTenant(@PathVariable Long tenantId) { return success(service.switchTenant(tenantId)); }

    @RequiresPermissions("system:tenant:add")
    @Log(title = "Tenant directory", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@Valid @RequestBody TenantDirectoryRequest request) { return success(service.create(request)); }

    @RequiresPermissions("system:tenant:edit")
    @Log(title = "Tenant directory", businessType = BusinessType.UPDATE)
    @PutMapping("/{tenantId}")
    public AjaxResult update(@PathVariable Long tenantId, @RequestBody TenantUpdateRequest request) { service.update(tenantId, request.version(), request.details()); return success(); }

    @RequiresPermissions("system:tenant:edit")
    @Log(title = "Tenant directory", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{tenantId}")
    public AjaxResult disable(@PathVariable Long tenantId, @RequestBody TenantVersionRequest request) { service.changeStatus(tenantId, request.version(), "1"); return success(); }

    public record TenantDirectoryRequest(@NotBlank @Size(max = 64) String code, @NotBlank @Size(max = 128) String name, @NotBlank @Size(max = 32) String plan, @Size(max = 128) String region) { }
    public record TenantUpdateRequest(Long version, @Valid TenantDirectoryRequest details) { }
    public record TenantVersionRequest(Long version) { }
}
