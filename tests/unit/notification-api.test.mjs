import test from "node:test";
import assert from "node:assert/strict";
import { createNotificationApi } from "../../src/features/notifications/notification-api.js";

test("notification and audit requests use the authenticated Gateway scope without browser tenant fields", async () => {
  const requests = [];
  const api = createNotificationApi({
    gatewayUrl: "https://gateway.example.com/",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requests.push({ url, init });
      return { ok: true, status: 200, json: async () => ({ code: 0, data: [] }) };
    },
  });

  await api.list({ unreadOnly: true });
  await api.markRead(18);
  await api.markAllRead();
  await api.listAuditEvents({ resourceType: "CARE_TASK", actorId: 45, limit: 20 });

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/notifications?unreadOnly=true");
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/notifications/18/read");
  assert.equal(requests[2].url, "https://gateway.example.com/care/api/v1/notifications/read-all");
  assert.equal(requests[3].url, "https://gateway.example.com/care/api/v1/audit-events?limit=20&resourceType=CARE_TASK&actorId=45");
  assert.equal(requests.every((request) => request.init.headers.Authorization === "Bearer access-token" && !request.url.includes("tenantId")), true);
});
