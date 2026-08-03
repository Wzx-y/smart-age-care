package com.ruoyi.system.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ruoyi.system.mapper.TenantMemberDirectoryMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TenantMemberDirectoryServiceTest
{
    private final TenantMemberDirectoryMapper mapper = Mockito.mock(TenantMemberDirectoryMapper.class);
    private final TenantMemberDirectoryService service = new TenantMemberDirectoryService(mapper);

    @Test
    void returnsActiveRuoYiRolePermissionsForAnEnabledTenantMember()
    {
        when(mapper.countActiveMembership(12L, 45L)).thenReturn(1);
        when(mapper.selectPermissionCodes(12L, 45L)).thenReturn(List.of("care:task:execute", "care:handover:receive"));

        var result = service.find(12L, 45L);

        assertTrue(result.active());
        assertTrue(result.permissions().contains("care:task:execute"));
    }

    @Test
    void returnsNoPermissionsWhenTheUserIsNotAnActiveTenantMember()
    {
        when(mapper.countActiveMembership(12L, 45L)).thenReturn(0);

        var result = service.find(12L, 45L);

        assertFalse(result.active());
        assertTrue(result.permissions().isEmpty());
    }

    @Test
    void addsAnActiveMembershipWithTenantScopedRoles()
    {
        when(mapper.countAvailableUser(45L)).thenReturn(1);
        when(mapper.countMembership(12L, 45L)).thenReturn(1);
        when(mapper.selectActiveRoleIds()).thenReturn(List.of(2L, 3L));

        service.addOrReactivate(12L, 45L, List.of(2L, 3L));

        org.mockito.Mockito.verify(mapper).deleteRoleAssignments(12L, 45L);
        org.mockito.Mockito.verify(mapper).insertRoleAssignments(12L, 45L, List.of(2L, 3L));
        org.mockito.Mockito.verify(mapper).upsertMembership(12L, 45L, "0");
    }

    @Test
    void rejectsARoleThatIsNotEnabledInRuoYi()
    {
        when(mapper.countAvailableUser(45L)).thenReturn(1);
        when(mapper.countMembership(12L, 45L)).thenReturn(1);
        when(mapper.selectActiveRoleIds()).thenReturn(List.of(2L));

        assertThrows(RuntimeException.class, () -> service.addOrReactivate(12L, 45L, Set.of(99L)));
    }
}
