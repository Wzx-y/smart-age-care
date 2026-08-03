package care.cloud.care.masterdata;

import care.cloud.care.security.CareAuthorizationService;
import care.cloud.care.shared.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/master-data")
public class MasterDataController {
    private final MasterDataService service;
    private final CareAuthorizationService authorization;

    public MasterDataController(MasterDataService service, CareAuthorizationService authorization) {
        this.service = service;
        this.authorization = authorization;
    }

    @GetMapping("/rooms")
    public ApiResponse<List<ManagedRoom>> listRooms(@RequestParam(defaultValue = "false") boolean includeDisabled) {
        requireManage();
        return ApiResponse.success(service.listRooms(includeDisabled));
    }

    @PostMapping("/rooms")
    public ApiResponse<ManagedRoom> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        requireManage();
        return ApiResponse.success(service.createRoom(request));
    }

    @PatchMapping("/rooms/{roomId}")
    public ApiResponse<ManagedRoom> updateRoom(@PathVariable Long roomId, @Valid @RequestBody UpdateRoomRequest request) {
        requireManage();
        return ApiResponse.success(service.updateRoom(roomId, request));
    }

    @PostMapping("/rooms/{roomId}/status")
    public ApiResponse<ManagedRoom> changeRoomStatus(@PathVariable Long roomId, @Valid @RequestBody ChangeMasterDataStatusRequest request) {
        requireManage();
        return ApiResponse.success(service.changeRoomStatus(roomId, request));
    }

    @GetMapping("/beds")
    public ApiResponse<List<ManagedBed>> listBeds(@RequestParam(defaultValue = "false") boolean includeDisabled) {
        requireManage();
        return ApiResponse.success(service.listBeds(includeDisabled));
    }

    @PostMapping("/beds")
    public ApiResponse<ManagedBed> createBed(@Valid @RequestBody CreateBedRequest request) {
        requireManage();
        return ApiResponse.success(service.createBed(request));
    }

    @PatchMapping("/beds/{bedId}")
    public ApiResponse<ManagedBed> updateBed(@PathVariable Long bedId, @Valid @RequestBody UpdateBedRequest request) {
        requireManage();
        return ApiResponse.success(service.updateBed(bedId, request));
    }

    @PostMapping("/beds/{bedId}/status")
    public ApiResponse<ManagedBed> changeBedStatus(@PathVariable Long bedId, @Valid @RequestBody ChangeMasterDataStatusRequest request) {
        requireManage();
        return ApiResponse.success(service.changeBedStatus(bedId, request));
    }

    @GetMapping("/catalogs/{category}")
    public ApiResponse<List<MasterDataItem>> listItems(@PathVariable MasterDataCategory category, @RequestParam(defaultValue = "false") boolean includeDisabled) {
        requireManage();
        return ApiResponse.success(service.listItems(category, includeDisabled));
    }

    @PostMapping("/catalogs/{category}")
    public ApiResponse<MasterDataItem> createItem(@PathVariable MasterDataCategory category, @Valid @RequestBody CreateMasterDataItemRequest request) {
        requireManage();
        return ApiResponse.success(service.createItem(category, request));
    }

    @PatchMapping("/catalogs/{category}/{itemId}")
    public ApiResponse<MasterDataItem> updateItem(@PathVariable MasterDataCategory category, @PathVariable Long itemId, @Valid @RequestBody UpdateMasterDataItemRequest request) {
        requireManage();
        return ApiResponse.success(service.updateItem(category, itemId, request));
    }

    @PostMapping("/catalogs/{category}/{itemId}/status")
    public ApiResponse<MasterDataItem> changeItemStatus(@PathVariable MasterDataCategory category, @PathVariable Long itemId, @Valid @RequestBody ChangeMasterDataStatusRequest request) {
        requireManage();
        return ApiResponse.success(service.changeItemStatus(category, itemId, request));
    }

    private void requireManage() {
        authorization.requireCurrent(CareAuthorizationService.MASTER_DATA_MANAGE);
    }
}
