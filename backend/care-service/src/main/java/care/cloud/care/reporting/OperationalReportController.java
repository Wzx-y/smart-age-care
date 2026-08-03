package care.cloud.care.reporting;

import care.cloud.care.security.CareAuthorizationService;
import care.cloud.care.shared.ApiResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operational-reports")
public class OperationalReportController {
    private final OperationalReportService service;
    private final CareAuthorizationService authorization;

    public OperationalReportController(OperationalReportService service, CareAuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping
    public ApiResponse<OperationalReport> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    ) {
        authorization.requireCurrent(CareAuthorizationService.REPORT_VIEW);
        return ApiResponse.success(service.report(periodStart, periodEnd));
    }
}
