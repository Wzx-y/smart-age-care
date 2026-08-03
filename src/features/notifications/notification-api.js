import { createApiClient } from "../../lib/api-client.js";

export function createNotificationApi({ gatewayUrl, getAccessToken, getRequestHeaders, fetchImpl }) {
  const client = createApiClient({ baseUrl: `${gatewayUrl.replace(/\/$/, "")}/care`, getAccessToken, getRequestHeaders, fetchImpl });

  return {
    list({ unreadOnly = false } = {}) {
      return client.request(`/api/v1/notifications?unreadOnly=${unreadOnly}`);
    },
    markRead(notificationId) {
      return client.request(`/api/v1/notifications/${encodeURIComponent(notificationId)}/read`, { method: "POST" });
    },
    markAllRead() {
      return client.request("/api/v1/notifications/read-all", { method: "POST" });
    },
    listAuditEvents({ resourceType, action, actorId, limit = 50 } = {}) {
      const query = new URLSearchParams({ limit: String(limit) });
      if (resourceType) query.set("resourceType", resourceType);
      if (action) query.set("action", action);
      if (actorId != null) query.set("actorId", String(actorId));
      return client.request(`/api/v1/audit-events?${query}`);
    },
  };
}
