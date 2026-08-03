import test from "node:test";
import assert from "node:assert/strict";
import { createCareApi } from "../../src/features/care/care-api.js";

test("care API sends the authenticated task completion contract without a tenant field", async () => {
  let requested;
  const api = createCareApi({
    gatewayUrl: "https://gateway.example.com/",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 200, json: async () => ({ code: 0, data: { id: 81, version: 5 } }) };
    },
    idempotencyKeyFactory: () => "care-task-complete-81",
  });

  await api.completeTask(81, { taskVersion: 4, resultNote: "已完成并观察皮肤情况" });

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/care-tasks/81/complete");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
  assert.equal(requested.init.headers["Idempotency-Key"], "care-task-complete-81");
  assert.deepEqual(JSON.parse(requested.init.body), { taskVersion: 4, resultNote: "已完成并观察皮肤情况" });
  assert.equal(requested.init.body.includes("tenantId"), false);
});

test("care API starts a versioned task through the authenticated Gateway scope", async () => {
  let requested;
  const api = createCareApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    idempotencyKeyFactory: () => "care-task-start-81",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, json: async () => ({ code: 0, data: { id: 81, status: "IN_PROGRESS" } }) };
    },
  });

  await api.startTask(81, { taskVersion: 4 });

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/care-tasks/81/start");
  assert.equal(requested.init.headers["Idempotency-Key"], "care-task-start-81");
  assert.equal(requested.init.body, '{"taskVersion":4}');
  assert.equal(requested.init.body.includes("tenantId"), false);
});

test("care API creates a task template through the authenticated tenant scope", async () => {
  let requested;
  const api = createCareApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    idempotencyKeyFactory: () => "template-51",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 200, json: async () => ({ code: 0, data: { id: 51 } }) };
    },
  });

  await api.createTaskTemplate({ planId: 31, taskName: "晨间巡视", scheduledTime: "08:30", assigneeId: 9 });

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/care-plan-task-templates");
  assert.equal(requested.init.method, "POST");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
  assert.equal(requested.init.headers["Idempotency-Key"], "template-51");
  assert.deepEqual(JSON.parse(requested.init.body), { planId: 31, taskName: "晨间巡视", scheduledTime: "08:30", assigneeId: 9 });
  assert.equal(requested.init.body.includes("tenantId"), false);
});

test("care API uses the plan subresource query without a browser tenant field", async () => {
  let requested;
  const api = createCareApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 200, json: async () => ({ code: 0, data: [] }) };
    },
  });

  await api.listTaskTemplates(31);

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/care-plan-task-templates?planId=31");
  assert.equal(requested.init.method, "GET");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
  assert.equal(requested.url.includes("tenantId"), false);
});

test("care API writes versioned maintenance commands and forwards service-record filters", async () => {
  const requests = [];
  const api = createCareApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    idempotencyKeyFactory: () => "care-maintenance-1",
    fetchImpl: async (url, options) => {
      requests.push({ url, options });
      return { ok: true, json: async () => ({ code: 0, data: [] }) };
    },
  });

  await api.updatePlan(71, { planVersion: 3, planName: "晚间照护", frequencyText: "每日", startDate: "2026-08-01" });
  await api.deactivateTaskTemplate(31, 2);
  await api.listServiceRecords({ residentId: 91, serviceDate: "2026-07-31", executorId: 45 });

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/care-plans/71");
  assert.equal(requests[0].options.method, "PATCH");
  assert.equal(requests[0].options.headers["Idempotency-Key"], "care-maintenance-1");
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/care-plan-task-templates/31/deactivate");
  assert.equal(requests[1].options.body, '{"templateVersion":2}');
  assert.equal(requests[2].url, "https://gateway.example.com/care/api/v1/care-service-records?residentId=91&serviceDate=2026-07-31&executorId=45");
  assert.equal(requests.every((request) => !request.options.body || !request.options.body.includes("tenantId")), true);
});
