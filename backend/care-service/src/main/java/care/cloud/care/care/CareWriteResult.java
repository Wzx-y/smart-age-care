package care.cloud.care.care;

public record CareWriteResult(String resourceType, Long resourceId, String status, long version, String detail) {
    public static CareWriteResult plan(CarePlan plan) {
        return new CareWriteResult("PLAN", plan.id(), plan.status().name(), plan.version(), null);
    }

    public static CareWriteResult task(CareTask task) {
        return new CareWriteResult("TASK", task.id(), task.status().name(), task.version(), task.exceptionReason());
    }

    public static CareWriteResult followUp(CareTaskFollowUp followUp) {
        return new CareWriteResult("FOLLOW_UP", followUp.id(), followUp.status().name(), followUp.version(), followUp.resolutionNote());
    }

    public static CareWriteResult template(CarePlanTaskTemplate template) {
        return new CareWriteResult("TEMPLATE", template.id(), template.active() ? "ACTIVE" : "INACTIVE", template.version(), null);
    }

    public static CareWriteResult generationRun(CareTaskGenerationRun run) {
        return new CareWriteResult("TASK_GENERATION_RUN", run.id(), run.serviceDate().toString(), 0, String.valueOf(run.generatedTaskCount()));
    }

    public static CareWriteResult handover(CareShiftHandover handover) {
        return new CareWriteResult("SHIFT_HANDOVER", handover.id(), handover.status().name(), handover.version(), null);
    }
}
