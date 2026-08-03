import { createApiClient } from "../../lib/api-client.js";

export function createCareApi({ gatewayUrl, getAccessToken, getRequestHeaders, fetchImpl, idempotencyKeyFactory = () => crypto.randomUUID() }) {
  const client = createApiClient({ baseUrl: `${gatewayUrl.replace(/\/$/, "")}/care`, getAccessToken, getRequestHeaders, fetchImpl });

  return {
    listPlans() {
      return client.request("/api/v1/care-plans");
    },
    listTasks() {
      return client.request("/api/v1/care-tasks");
    },
    listServiceRecords({ residentId, serviceDate, executorId } = {}) {
      const query = new URLSearchParams();
      if (residentId != null) query.set("residentId", residentId);
      if (serviceDate) query.set("serviceDate", serviceDate);
      if (executorId != null) query.set("executorId", executorId);
      const suffix = query.size ? `?${query}` : "";
      return client.request(`/api/v1/care-service-records${suffix}`);
    },
    createPlan({ residentId, planName, frequencyText, startDate, endDate, assessmentId }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request("/api/v1/care-plans", {
        method: "POST",
        body: { residentId, planName, frequencyText, startDate, ...(endDate ? { endDate } : {}), ...(assessmentId ? { assessmentId } : {}) },
        idempotencyKey,
      });
    },
    publishPlan(planId, planVersion, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-plans/${encodeURIComponent(planId)}/publish`, {
        method: "POST",
        body: { planVersion },
        idempotencyKey,
      });
    },
    updatePlan(planId, payload, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-plans/${encodeURIComponent(planId)}`, { method: "PATCH", body: payload, idempotencyKey });
    },
    cancelPlan(planId, planVersion, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-plans/${encodeURIComponent(planId)}/cancel`, { method: "POST", body: { planVersion }, idempotencyKey });
    },
    listTaskTemplates(planId) {
      return client.request(`/api/v1/care-plan-task-templates?planId=${encodeURIComponent(planId)}`);
    },
    createTaskTemplate({ planId, taskName, scheduledTime, assigneeId }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request("/api/v1/care-plan-task-templates", {
        method: "POST",
        body: { planId, taskName, scheduledTime, assigneeId },
        idempotencyKey,
      });
    },
    updateTaskTemplate(templateId, payload, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-plan-task-templates/${encodeURIComponent(templateId)}`, { method: "PATCH", body: payload, idempotencyKey });
    },
    deactivateTaskTemplate(templateId, templateVersion, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-plan-task-templates/${encodeURIComponent(templateId)}/deactivate`, { method: "POST", body: { templateVersion }, idempotencyKey });
    },
    generateTasks(serviceDate, idempotencyKey = idempotencyKeyFactory()) {
      return client.request("/api/v1/care-task-generation-runs", {
        method: "POST",
        body: { serviceDate },
        idempotencyKey,
      });
    },
    createTask({ planId, taskName, scheduledAt, assigneeId }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request("/api/v1/care-tasks", {
        method: "POST",
        body: { planId, taskName, scheduledAt, assigneeId },
        idempotencyKey,
      });
    },
    startTask(taskId, { taskVersion }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-tasks/${encodeURIComponent(taskId)}/start`, {
        method: "POST",
        body: { taskVersion },
        idempotencyKey,
      });
    },
    completeTask(taskId, { taskVersion, resultNote }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-tasks/${encodeURIComponent(taskId)}/complete`, {
        method: "POST",
        body: { taskVersion, resultNote },
        idempotencyKey,
      });
    },
    markTaskException(taskId, { taskVersion, exceptionReason }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-tasks/${encodeURIComponent(taskId)}/exception`, {
        method: "POST",
        body: { taskVersion, exceptionReason },
        idempotencyKey,
      });
    },
    listTaskFollowUps(status) {
      const query = status ? `?status=${encodeURIComponent(status)}` : "";
      return client.request(`/api/v1/care-task-follow-ups${query}`);
    },
    resolveTaskFollowUp(followUpId, { followUpVersion, resolutionNote }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-task-follow-ups/${encodeURIComponent(followUpId)}/resolve`, {
        method: "POST",
        body: { followUpVersion, resolutionNote },
        idempotencyKey,
      });
    },
    listShiftHandovers() {
      return client.request("/api/v1/care-shift-handovers");
    },
    createShiftHandover({ shiftDate, shiftCode, toUserId, note, taskIds }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request("/api/v1/care-shift-handovers", {
        method: "POST",
        body: { shiftDate, shiftCode, toUserId, note, taskIds },
        idempotencyKey,
      });
    },
    submitShiftHandover(handoverId, handoverVersion, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/care-shift-handovers/${encodeURIComponent(handoverId)}/submit`, {
        method: "POST",
        body: { handoverVersion },
        idempotencyKey,
      });
    },
  };
}
