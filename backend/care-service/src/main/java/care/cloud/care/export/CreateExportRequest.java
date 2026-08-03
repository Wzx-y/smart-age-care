package care.cloud.care.export;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateExportRequest(@NotNull ExportType exportType, @NotNull LocalDate periodStart, @NotNull LocalDate periodEnd) {
}
