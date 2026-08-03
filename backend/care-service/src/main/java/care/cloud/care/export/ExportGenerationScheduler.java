package care.cloud.care.export;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExportGenerationScheduler {
    private final ExportService exportService;

    public ExportGenerationScheduler(ExportService exportService) {
        this.exportService = exportService;
    }

    @Scheduled(fixedDelayString = "${exports.generation-scan-ms:5000}")
    public void generateAndExpire() {
        exportService.expireReadyExports();
        exportService.generateNext();
    }
}
