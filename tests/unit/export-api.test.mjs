import test from "node:test";
import assert from "node:assert/strict";
import { createExportApi } from "../../src/features/exports/export-api.js";

test("report and export API use authenticated Gateway requests without a tenant field", async () => {
  const requests = [];
  const api = createExportApi({
    gatewayUrl: "https://gateway.example.com/",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requests.push({ url, init });
      return { ok: true, status: 200, json: async () => ({ code: 0, data: {} }) };
    },
  });

  await api.getOperationalReport({ periodStart: "2026-07-01", periodEnd: "2026-07-31" });
  await api.listExports();
  await api.createExport({ exportType: "OPERATIONAL_SUMMARY_CSV", periodStart: "2026-07-01", periodEnd: "2026-07-31" });
  await api.getExportAccess(71);

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/operational-reports?periodStart=2026-07-01&periodEnd=2026-07-31");
  assert.equal(requests[0].init.headers.Authorization, "Bearer access-token");
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/exports");
  assert.equal(requests[1].init.method, "GET");
  assert.equal(requests[2].url, "https://gateway.example.com/care/api/v1/exports");
  assert.deepEqual(JSON.parse(requests[2].init.body), { exportType: "OPERATIONAL_SUMMARY_CSV", periodStart: "2026-07-01", periodEnd: "2026-07-31" });
  assert.equal(requests[3].url, "https://gateway.example.com/care/api/v1/exports/71/access-url");
  assert.equal(requests.every((request) => !request.url.includes("tenantId") && (!request.init.body || !request.init.body.includes("tenantId"))), true);
});
