import test from "node:test";
import assert from "node:assert/strict";
import { createMasterDataApi } from "../../src/features/master-data/master-data-api.js";

test("master-data API uses the authenticated Gateway contract without a browser tenant field", async () => {
  const requests = [];
  const api = createMasterDataApi({
    gatewayUrl: "https://gateway.example.com/",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requests.push({ url, init });
      return { ok: true, status: 200, json: async () => ({ code: 0, data: [] }) };
    },
  });

  await api.createBed({ roomId: 71, bedNo: "A", equipmentSummary: "护理呼叫器" });
  await api.changeCatalogItemStatus("RISK_LEVEL", 91, { version: 2, enabled: false });
  await api.listRooms({ includeDisabled: true });

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/master-data/beds");
  assert.equal(requests[0].init.headers.Authorization, "Bearer access-token");
  assert.deepEqual(JSON.parse(requests[0].init.body), { roomId: 71, bedNo: "A", equipmentSummary: "护理呼叫器" });
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/master-data/catalogs/RISK_LEVEL/91/status");
  assert.equal(requests[1].init.body, '{"version":2,"enabled":false}');
  assert.equal(requests[2].url, "https://gateway.example.com/care/api/v1/master-data/rooms?includeDisabled=true");
  assert.equal(requests.every((request) => !request.url.includes("tenantId") && (!request.init.body || !request.init.body.includes("tenantId"))), true);
});
