package care.cloud.care.care;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;

public final class CareRequestFingerprint {
    private CareRequestFingerprint() {
    }

    public static String forCreatePlan(CreateCarePlanRequest request) {
        return sha256("CREATE_CARE_PLAN|" + request.residentId() + "|" + request.planName() + "|" + request.frequencyText()
                + "|" + request.startDate() + "|" + request.endDate() + "|" + request.assessmentId());
    }

    public static String forPublishPlan(Long planId, PublishCarePlanRequest request) {
        return sha256("PUBLISH_CARE_PLAN|" + planId + "|" + request.planVersion());
    }

    public static String forUpdatePlan(Long planId, UpdateCarePlanRequest request) {
        return sha256("UPDATE_CARE_PLAN|" + planId + "|" + request.planVersion() + "|" + request.planName() + "|" + request.frequencyText() + "|" + request.startDate() + "|" + request.endDate() + "|" + request.assessmentId());
    }

    public static String forCancelPlan(Long planId, ChangeCarePlanStatusRequest request) {
        return sha256("CANCEL_CARE_PLAN|" + planId + "|" + request.planVersion());
    }

    public static String forCreateTask(CreateCareTaskRequest request) {
        return sha256("CREATE_CARE_TASK|" + request.planId() + "|" + request.taskName() + "|" + request.scheduledAt() + "|" + request.assigneeId());
    }

    public static String forCompleteTask(Long taskId, CompleteCareTaskRequest request) {
        return sha256("COMPLETE_CARE_TASK|" + taskId + "|" + request.taskVersion() + "|" + request.resultNote());
    }

    public static String forStartTask(Long taskId, StartCareTaskRequest request) {
        return sha256("START_CARE_TASK|" + taskId + "|" + request.taskVersion());
    }

    public static String forTaskException(Long taskId, MarkCareTaskExceptionRequest request) {
        return sha256("MARK_CARE_TASK_EXCEPTION|" + taskId + "|" + request.taskVersion() + "|" + request.exceptionReason());
    }

    public static String forResolveFollowUp(Long followUpId, ResolveCareTaskFollowUpRequest request) {
        return sha256("RESOLVE_CARE_TASK_FOLLOW_UP|" + followUpId + "|" + request.followUpVersion() + "|" + request.resolutionNote());
    }

    public static String forCreateTemplate(CreateCarePlanTaskTemplateRequest request) {
        return sha256("CREATE_CARE_PLAN_TASK_TEMPLATE|" + request.planId() + "|" + request.taskName() + "|" + request.scheduledTime() + "|" + request.assigneeId());
    }

    public static String forUpdateTemplate(Long templateId, UpdateCarePlanTaskTemplateRequest request) {
        return sha256("UPDATE_CARE_PLAN_TASK_TEMPLATE|" + templateId + "|" + request.templateVersion() + "|" + request.taskName() + "|" + request.scheduledTime() + "|" + request.assigneeId());
    }

    public static String forDeactivateTemplate(Long templateId, ChangeCarePlanTaskTemplateStatusRequest request) {
        return sha256("DEACTIVATE_CARE_PLAN_TASK_TEMPLATE|" + templateId + "|" + request.templateVersion());
    }

    public static String forGenerateTasks(GenerateCareTasksRequest request) {
        return sha256("GENERATE_CARE_TASKS|" + request.serviceDate());
    }

    public static String forCreateShiftHandover(CreateCareShiftHandoverRequest request) {
        String taskIds = request.taskIds().stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        return sha256("CREATE_SHIFT_HANDOVER|" + request.shiftDate() + "|" + request.shiftCode().trim()
                + "|" + request.toUserId() + "|" + request.note().trim() + "|" + taskIds);
    }

    public static String forSubmitShiftHandover(Long handoverId, SubmitCareShiftHandoverRequest request) {
        return sha256("SUBMIT_SHIFT_HANDOVER|" + handoverId + "|" + request.handoverVersion());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持 SHA-256", exception);
        }
    }
}
