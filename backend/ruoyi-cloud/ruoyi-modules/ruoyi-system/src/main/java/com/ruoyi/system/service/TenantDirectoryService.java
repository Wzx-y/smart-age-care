package com.ruoyi.system.service;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.controller.TenantDirectoryController.TenantDirectoryRequest;
import com.ruoyi.system.mapper.TenantDirectoryMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantDirectoryService {
    private final TenantDirectoryMapper mapper;

    public TenantDirectoryService(TenantDirectoryMapper mapper) { this.mapper = mapper; }

    public List<TenantDirectoryMapper.TenantDirectorySummary> listMine() { return mapper.selectForUser(SecurityUtils.getUserId()); }

    public TenantDirectoryMapper.TenantDirectorySummary switchTenant(Long tenantId) {
        requireMembership(tenantId);
        return mapper.selectForUser(SecurityUtils.getUserId()).stream()
                .filter(item -> item.tenantId().equals(tenantId))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Tenant is not available to the current user"));
    }

    @Transactional
    public TenantDirectoryMapper.TenantDirectorySummary create(TenantDirectoryRequest request) {
        if (request == null || request.code() == null || request.code().isBlank() || request.name() == null || request.name().isBlank()) throw new ServiceException("Tenant code and name are required");
        mapper.insert(request);
        Long tenantId = mapper.lastInsertId();
        if (tenantId == null) throw new ServiceException("Tenant creation did not return an id");
        mapper.addOwner(tenantId, SecurityUtils.getUserId());
        return mapper.selectForUser(SecurityUtils.getUserId()).stream().filter(item -> item.tenantId().equals(tenantId)).findFirst().orElseThrow();
    }

    public void update(Long tenantId, Long version, TenantDirectoryRequest request) {
        requireMembership(tenantId);
        validate(request);
        if (version == null) throw new ServiceException("Tenant version is required");
        if (mapper.update(tenantId, version, request) != 1) throw new ServiceException("Tenant version conflict");
    }

    public void changeStatus(Long tenantId, Long version, String status) {
        requireMembership(tenantId);
        if (version == null) throw new ServiceException("Tenant version is required");
        if (!"0".equals(status) && !"1".equals(status)) throw new ServiceException("Tenant status is invalid");
        if (mapper.updateStatus(tenantId, version, status) != 1) throw new ServiceException("Tenant version conflict");
    }

    private void requireMembership(Long tenantId) {
        if (mapper.selectForUser(SecurityUtils.getUserId()).stream().noneMatch(item -> item.tenantId().equals(tenantId))) throw new ServiceException("Tenant is not available to the current user");
    }

    private void validate(TenantDirectoryRequest request) {
        if (request == null || request.code() == null || request.code().isBlank()
                || request.name() == null || request.name().isBlank()
                || request.plan() == null || request.plan().isBlank()) {
            throw new ServiceException("Tenant code, name and plan are required");
        }
    }
}
