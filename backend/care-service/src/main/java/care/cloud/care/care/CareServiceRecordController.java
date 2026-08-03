package care.cloud.care.care;

import care.cloud.care.security.CareAuthorizationService;
import care.cloud.care.shared.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/care-service-records")
public class CareServiceRecordController {
    private final CareServiceRecordQueryService service;
    private final CareAuthorizationService authorizationService;

    public CareServiceRecordController(CareServiceRecordQueryService service, CareAuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<List<CareServiceRecord>> list(@RequestParam(required = false) Long residentId,
                                                      @RequestParam(required = false) LocalDate serviceDate,
                                                      @RequestParam(required = false) Long executorId) {
        authorizationService.requireCurrent(CareAuthorizationService.TASK_MANAGE);
        return ApiResponse.success(service.list(residentId, serviceDate, executorId));
    }
}
