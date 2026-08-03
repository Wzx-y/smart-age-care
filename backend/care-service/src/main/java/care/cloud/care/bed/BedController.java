package care.cloud.care.bed;

import care.cloud.care.shared.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/beds")
public class BedController {
    private final BedQueryService bedQueryService;
    private final BedCleaningService bedCleaningService;

    public BedController(BedQueryService bedQueryService, BedCleaningService bedCleaningService) {
        this.bedQueryService = bedQueryService;
        this.bedCleaningService = bedCleaningService;
    }

    @GetMapping
    public ApiResponse<List<BedSummary>> list(
            @RequestParam(value = "occupancyStatus", required = false) BedOccupancyStatus occupancyStatus
    ) {
        return ApiResponse.success(bedQueryService.list(occupancyStatus));
    }

    @PostMapping("/{bedId}/complete-cleaning")
    public ApiResponse<Bed> completeCleaning(@PathVariable Long bedId, @Valid @RequestBody CompleteBedCleaningRequest request) {
        return ApiResponse.success(bedCleaningService.complete(bedId, request));
    }
}
