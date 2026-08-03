package care.cloud.care.care;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CareTaskGenerationServiceTest {
    private final CarePlanTaskTemplateRepository templateRepository = Mockito.mock(CarePlanTaskTemplateRepository.class);
    private final CareTaskRepository taskRepository = Mockito.mock(CareTaskRepository.class);
    private final CareTaskGenerationRunRepository runRepository = Mockito.mock(CareTaskGenerationRunRepository.class);
    private final CareTaskGenerationService service = new CareTaskGenerationService(templateRepository, taskRepository, runRepository);

    @AfterEach
    void clearTenantContext() { TenantContext.clear(); }

    @Test
    void generateCreatesOnlyTemplateTasksNotAlreadyPresentForTheServiceDate() {
        TenantContext.set(new TenantPrincipal(45L, 12L));
        LocalDate date = LocalDate.of(2026, 8, 2);
        CarePlanTaskTemplate first = new CarePlanTaskTemplate(101L, 12L, 71L, 91L, "晨间翻身", LocalTime.of(8, 0), 52L, true, 0);
        CarePlanTaskTemplate second = new CarePlanTaskTemplate(102L, 12L, 71L, 91L, "血压测量", LocalTime.of(10, 0), 52L, true, 0);
        when(templateRepository.findActiveForPublishedPlans(12L, date)).thenReturn(List.of(first, second));
        when(taskRepository.createFromTemplate(first, date)).thenReturn(true);
        when(taskRepository.createFromTemplate(second, date)).thenReturn(false);
        CareTaskGenerationRun run = new CareTaskGenerationRun(31L, 12L, date, 1, 45L, OffsetDateTime.now(ZoneOffset.UTC));
        when(runRepository.create(Mockito.any())).thenReturn(run);

        CareTaskGenerationRun result = service.generate(new GenerateCareTasksRequest(date));

        verify(taskRepository).createFromTemplate(first, date);
        verify(taskRepository).createFromTemplate(second, date);
        assertEquals(1, result.generatedTaskCount());
    }
}
