package care.cloud.care.export;

import care.cloud.care.security.CareAuthorizationService;
import care.cloud.care.shared.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportController {
    private final ExportService service;
    private final CareAuthorizationService authorization;

    public ExportController(ExportService service, CareAuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping
    public ApiResponse<List<ExportJobView>> list() {
        authorization.requireCurrent(CareAuthorizationService.EXPORT_MANAGE);
        return ApiResponse.success(service.list().stream().map(ExportJobView::from).toList());
    }

    @PostMapping
    public ApiResponse<ExportJobView> create(@Valid @RequestBody CreateExportRequest request) {
        authorization.requireCurrent(CareAuthorizationService.EXPORT_MANAGE);
        return ApiResponse.success(ExportJobView.from(service.create(request)));
    }

    @PostMapping("/{exportId}/access-url")
    public ApiResponse<ExportAccessTarget> access(@PathVariable Long exportId) {
        authorization.requireCurrent(CareAuthorizationService.EXPORT_DOWNLOAD);
        return ApiResponse.success(service.access(exportId));
    }
}
