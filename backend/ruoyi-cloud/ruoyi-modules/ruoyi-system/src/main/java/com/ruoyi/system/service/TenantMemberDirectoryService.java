package com.ruoyi.system.service;

import com.ruoyi.system.mapper.TenantMemberDirectoryMapper;
import com.ruoyi.common.core.exception.ServiceException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TenantMemberDirectoryService
{
    private final TenantMemberDirectoryMapper mapper;

    public TenantMemberDirectoryService(TenantMemberDirectoryMapper mapper)
    {
        this.mapper = mapper;
    }

    public TenantMemberDirectoryResult find(Long tenantId, Long userId)
    {
        boolean active = mapper.countActiveMembership(tenantId, userId) == 1;
        Set<String> permissions = active ? new LinkedHashSet<>(mapper.selectPermissionCodes(tenantId, userId)) : Set.of();
        return new TenantMemberDirectoryResult(tenantId, userId, active, permissions);
    }

    public List<TenantMemberDirectoryMapper.TenantMemberSummary> list(Long tenantId)
    {
        requireTenantId(tenantId);
        return mapper.selectMembers(tenantId);
    }

    @Transactional
    public void addOrReactivate(Long tenantId, Long userId, Collection<Long> roleIds)
    {
        requireTenantId(tenantId);
        requireAvailableUser(userId);
        mapper.upsertMembership(tenantId, userId, "0");
        replaceRoles(tenantId, userId, roleIds);
    }

    @Transactional
    public void replaceRoles(Long tenantId, Long userId, Collection<Long> roleIds)
    {
        requireTenantId(tenantId);
        requireAvailableUser(userId);
        if (mapper.countMembership(tenantId, userId) != 1)
        {
            throw new ServiceException("Tenant membership does not exist");
        }
        List<Long> normalizedRoleIds = normalizeRoleIds(roleIds);
        Set<Long> activeRoleIds = new LinkedHashSet<>(mapper.selectActiveRoleIds());
        if (!activeRoleIds.containsAll(normalizedRoleIds))
        {
            throw new ServiceException("Role does not exist or is disabled");
        }
        mapper.deleteRoleAssignments(tenantId, userId);
        mapper.insertRoleAssignments(tenantId, userId, normalizedRoleIds);
    }

    public void changeStatus(Long tenantId, Long userId, String status)
    {
        requireTenantId(tenantId);
        requireAvailableUser(userId);
        if (!"0".equals(status) && !"1".equals(status))
        {
            throw new ServiceException("Member status must be 0 or 1");
        }
        if (mapper.updateMembershipStatus(tenantId, userId, status) != 1)
        {
            throw new ServiceException("Member does not exist in the current tenant");
        }
    }

    private void requireTenantId(Long tenantId)
    {
        if (tenantId == null || tenantId <= 0)
        {
            throw new ServiceException("Trusted tenant context is invalid");
        }
    }

    private void requireAvailableUser(Long userId)
    {
        if (userId == null || userId <= 0 || mapper.countAvailableUser(userId) != 1)
        {
            throw new ServiceException("User does not exist or has been deleted");
        }
    }

    private List<Long> normalizeRoleIds(Collection<Long> roleIds)
    {
        if (roleIds == null)
        {
            throw new ServiceException("A tenant member requires at least one enabled role");
        }
        List<Long> normalized = roleIds.stream().filter(roleId -> roleId != null && roleId > 0).distinct().toList();
        if (normalized.isEmpty() || normalized.size() != roleIds.size())
        {
            throw new ServiceException("Tenant member roles are invalid");
        }
        return normalized;
    }

    public record TenantMemberDirectoryResult(Long tenantId, Long userId, boolean active, Set<String> permissions)
    {
    }
}
