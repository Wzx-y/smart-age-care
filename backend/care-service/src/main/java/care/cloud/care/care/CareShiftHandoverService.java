package care.cloud.care.care;

import care.cloud.care.security.TenantContext;
import care.cloud.care.notification.NotificationCategory;
import care.cloud.care.notification.NotificationPriority;
import care.cloud.care.notification.NotificationService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareShiftHandoverService {
    private final CareShiftHandoverRepository handoverRepository;
    private final CareTaskRepository taskRepository;
    private final CareTaskFollowUpRepository followUpRepository;
    private final NotificationService notificationService;

    public CareShiftHandoverService(CareShiftHandoverRepository handoverRepository, CareTaskRepository taskRepository,
                                    CareTaskFollowUpRepository followUpRepository, NotificationService notificationService) {
        this.handoverRepository = handoverRepository;
        this.taskRepository = taskRepository;
        this.followUpRepository = followUpRepository;
        this.notificationService = notificationService;
    }

    public List<CareShiftHandover> list() {
        var principal = TenantContext.requireCurrent();
        return handoverRepository.findByTenantId(principal.tenantId()).stream().map(this::withTaskIds).toList();
    }

    @Transactional(rollbackFor = CareShiftHandoverConflictException.class)
    public CareShiftHandover create(CreateCareShiftHandoverRequest request) {
        var principal = TenantContext.requireCurrent();
        String shiftCode = request.shiftCode().trim();
        String note = request.note().trim();
        List<Long> taskIds = uniqueTaskIds(request.taskIds());
        if (handoverRepository.findByShiftAndFromUser(principal.tenantId(), request.shiftDate(), shiftCode, principal.userId()).isPresent()) {
            throw new CareShiftHandoverConflictException("当前班次已存在交接草稿或已提交交接");
        }
        List<CareTask> tasks = taskRepository.findByIdsAndTenantId(taskIds, principal.tenantId());
        if (tasks.size() != taskIds.size()) throw new CareNotFoundException("护理任务", 0L);
        if (tasks.stream().anyMatch(task -> task.status() != CareTaskStatus.COMPLETED && task.status() != CareTaskStatus.EXCEPTION)) {
            throw new CareShiftHandoverConflictException("仅已完成或异常的护理任务可以进入班次交接");
        }
        CareShiftHandover created = handoverRepository.create(new CareShiftHandover(null, principal.tenantId(), request.shiftDate(),
                shiftCode, principal.userId(), request.toUserId(), note, CareShiftHandoverStatus.DRAFT, null, null, null, 0, List.of()));
        handoverRepository.createItems(principal.tenantId(), created.id(), taskIds);
        return withTaskIds(created);
    }

    @Transactional(rollbackFor = CareStateConflictException.class)
    public CareShiftHandover submit(Long handoverId, SubmitCareShiftHandoverRequest request) {
        var principal = TenantContext.requireCurrent();
        CareShiftHandover handover = handoverRepository.findByIdAndTenantId(handoverId, principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("班次交接", handoverId));
        if (handover.version() != request.handoverVersion() || handover.status() != CareShiftHandoverStatus.DRAFT) {
            throw new CareStateConflictException();
        }
        List<Long> taskIds = handoverRepository.findTaskIds(handover.id(), principal.tenantId());
        if (followUpRepository.countOpenByTaskIdsAndTenantId(taskIds, principal.tenantId()) > 0) {
            throw new CareShiftHandoverConflictException("存在未结案的异常跟进项，不能提交班次交接");
        }
        if (!handoverRepository.submit(handover)) throw new CareStateConflictException();
        notificationService.publishToMember(principal.tenantId(), handover.toUserId(), NotificationCategory.HANDOVER, NotificationPriority.NORMAL,
                "班次交接待接收", "SHIFT_HANDOVER", handover.id(), null, "HANDOVER_SUBMITTED:" + handover.id());
        return new CareShiftHandover(handover.id(), handover.tenantId(), handover.shiftDate(), handover.shiftCode(), handover.fromUserId(),
                handover.toUserId(), handover.note(), CareShiftHandoverStatus.SUBMITTED, OffsetDateTime.now(ZoneOffset.UTC),
                handover.createdAt(), OffsetDateTime.now(ZoneOffset.UTC), handover.version() + 1, taskIds);
    }

    public CareShiftHandover replay(CareWriteResult result) {
        if (!"SHIFT_HANDOVER".equals(result.resourceType())) throw new CareStateConflictException();
        var principal = TenantContext.requireCurrent();
        return withTaskIds(handoverRepository.findByIdAndTenantId(result.resourceId(), principal.tenantId())
                .orElseThrow(() -> new CareNotFoundException("班次交接", result.resourceId())));
    }

    private CareShiftHandover withTaskIds(CareShiftHandover handover) {
        return new CareShiftHandover(handover.id(), handover.tenantId(), handover.shiftDate(), handover.shiftCode(), handover.fromUserId(),
                handover.toUserId(), handover.note(), handover.status(), handover.submittedAt(), handover.createdAt(), handover.updatedAt(),
                handover.version(), handoverRepository.findTaskIds(handover.id(), handover.tenantId()));
    }

    private List<Long> uniqueTaskIds(List<Long> taskIds) {
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(taskIds));
        if (uniqueIds.size() != taskIds.size()) throw new IllegalArgumentException("交接任务不能重复");
        return uniqueIds;
    }
}
