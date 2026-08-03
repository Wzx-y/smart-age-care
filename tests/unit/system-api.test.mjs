import assert from "node:assert/strict";
import test from "node:test";
import { createSystemApi } from "../../src/features/system/system-api.js";

test("system API reads tenant members through the Gateway-selected tenant context", async () => {
  let request;
  const api = createSystemApi({
    gatewayUrl: "https://gateway.example.com/",
    getAccessToken: () => "access-token",
    getRequestHeaders: () => ({ "X-Tenant-Id": "12" }),
    fetchImpl: async (url, options) => {
      request = { url, options };
      return { ok: true, json: async () => ({ code: 200, data: [{ userId: 45 }] }) };
    },
  });

  const members = await api.listMembers();

  assert.deepEqual(members, [{ userId: 45 }]);
  assert.equal(request.url, "https://gateway.example.com/system/tenant-members");
  assert.equal(request.options.headers.Authorization, "Bearer access-token");
  assert.equal(request.options.headers["X-Tenant-Id"], "12");
});

test("system API changes membership and roles without placing the tenant in request data", async () => {
  const requests = [];
  const api = createSystemApi({
    gatewayUrl: "https://gateway.example.com",
    getRequestHeaders: () => ({ "X-Tenant-Id": "12" }),
    fetchImpl: async (url, options) => {
      requests.push({ url, options });
      return { ok: true, json: async () => ({ code: 200, data: null }) };
    },
  });

  await api.addMember({ userId: 45, roleIds: [2, 3] });
  await api.replaceMemberRoles(45, [3]);
  await api.changeMemberStatus(45, "1");

  assert.equal(requests[0].url, "https://gateway.example.com/system/tenant-members");
  assert.deepEqual(JSON.parse(requests[0].options.body), { userId: 45, roleIds: [2, 3] });
  assert.equal(requests[1].url, "https://gateway.example.com/system/tenant-members/45/roles");
  assert.deepEqual(JSON.parse(requests[1].options.body), { roleIds: [3] });
  assert.equal(requests[2].url, "https://gateway.example.com/system/tenant-members/45/status");
  assert.deepEqual(JSON.parse(requests[2].options.body), { status: "1" });
  assert.equal(requests.every((request) => !request.options.body.includes("tenantId")), true);
  assert.equal(requests.every((request) => request.options.headers["X-Tenant-Id"] === "12"), true);
});

test("system API maintains the tenant directory through RuoYi without browser tenant fields", async () => {
  const requests = [];
  const api = createSystemApi({
    gatewayUrl: "https://gateway.example.com/",
    getAccessToken: () => "access-token",
    getRequestHeaders: () => ({ "X-Tenant-Id": "12" }),
    fetchImpl: async (url, options) => {
      requests.push({ url, options });
      return { ok: true, status: 200, json: async () => ({ code: 200, data: [{ tenantId: 12, code: "DEMO", name: "示例机构" }] }) };
    },
  });

  const tenants = await api.listTenantDirectory();
  await api.createTenant({ code: "NEW", name: "新机构", plan: "STANDARD", region: "华东" });
  await api.updateTenant(12, { version: 2, details: { code: "DEMO", name: "更新机构", plan: "PROFESSIONAL", region: "华东" } });
  await api.disableTenant(12, 3);

  assert.deepEqual(tenants, [{ tenantId: 12, code: "DEMO", name: "示例机构" }]);
  assert.equal(requests[0].url, "https://gateway.example.com/system/tenant-directory");
  assert.equal(requests[1].options.method, "POST");
  assert.deepEqual(JSON.parse(requests[1].options.body), { code: "NEW", name: "新机构", plan: "STANDARD", region: "华东" });
  assert.equal(requests[2].url, "https://gateway.example.com/system/tenant-directory/12");
  assert.equal(requests[2].options.method, "PUT");
  assert.equal(requests[3].options.method, "DELETE");
  assert.equal(requests.every(({ options }) => !String(options.body || "").includes("tenantId")), true);
  assert.equal(requests.every(({ options }) => options.headers.Authorization === "Bearer access-token"), true);
});
