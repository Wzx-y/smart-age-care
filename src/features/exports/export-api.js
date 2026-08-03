import { createApiClient } from "../../lib/api-client.js";

export function createExportApi({ gatewayUrl, getAccessToken, getRequestHeaders, fetchImpl }) {
  const client = createApiClient({ baseUrl: `${gatewayUrl.replace(/\/$/, "")}/care`, getAccessToken, getRequestHeaders, fetchImpl });

  return {
    getOperationalReport({ periodStart, periodEnd }) {
      const query = new URLSearchParams({ periodStart, periodEnd });
      return client.request(`/api/v1/operational-reports?${query}`);
    },
    listExports() {
      return client.request("/api/v1/exports");
    },
    createExport({ exportType, periodStart, periodEnd }) {
      return client.request("/api/v1/exports", { method: "POST", body: { exportType, periodStart, periodEnd } });
    },
    getExportAccess(exportId) {
      return client.request(`/api/v1/exports/${encodeURIComponent(exportId)}/access-url`, { method: "POST" });
    },
  };
}
