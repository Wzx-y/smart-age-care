package com.ruoyi.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.controller.TenantDirectoryController.TenantDirectoryRequest;
import com.ruoyi.system.mapper.TenantDirectoryMapper;
import com.ruoyi.system.mapper.TenantDirectoryMapper.TenantDirectorySummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class TenantDirectoryServiceTest
{
    private final TenantDirectoryMapper mapper = Mockito.mock(TenantDirectoryMapper.class);
    private final TenantDirectoryService service = new TenantDirectoryService(mapper);

    @Test
    void createsDirectoryEntryAndMakesCurrentUserItsFirstMember()
    {
        TenantDirectoryRequest request = new TenantDirectoryRequest("EAST-001", "华东照护中心", "STANDARD", "华东");
        TenantDirectorySummary created = new TenantDirectorySummary(38L, "EAST-001", "华东照护中心", "STANDARD", "华东", "0", 0L, 1L);
        when(mapper.lastInsertId()).thenReturn(38L);
        when(mapper.selectForUser(45L)).thenReturn(List.of(created));

        try (MockedStatic<SecurityUtils> security = Mockito.mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(45L);

            assertEquals(created, service.create(request));
        }

        verify(mapper).insert(request);
        verify(mapper).addOwner(38L, 45L);
    }

    @Test
    void rejectsUpdateWithoutTheOptimisticVersion()
    {
        TenantDirectorySummary member = new TenantDirectorySummary(12L, "DEMO", "示例机构", "STANDARD", "华东", "0", 2L, 3L);
        when(mapper.selectForUser(45L)).thenReturn(List.of(member));

        try (MockedStatic<SecurityUtils> security = Mockito.mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(45L);

            assertThrows(RuntimeException.class, () -> service.update(12L, null,
                    new TenantDirectoryRequest("DEMO", "示例机构", "STANDARD", "华东")));
        }
    }
}
