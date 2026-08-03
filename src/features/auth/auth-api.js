import { createApiClient } from "../../lib/api-client.js";

export function createAuthApi({ gatewayUrl, getAccessToken, fetchImpl }) {
  const client = createApiClient({
    baseUrl: gatewayUrl,
    getAccessToken,
    fetchImpl,
  });

  return {
    login({ account, password }) {
      return client.request("/auth/login", {
        method: "POST",
        body: { account, password },
      });
    },
    refresh(refreshToken) {
      return client.request("/auth/refresh", {
        method: "POST",
        body: { refreshToken },
      });
    },
    getCurrentUser() {
      return client.request("/system/user/getInfo");
    },
    listTenants() {
      return client.request("/system/tenant-directory");
    },
    switchTenant(tenantId) {
      return client.request(`/system/tenant-directory/${encodeURIComponent(tenantId)}/switch`, { method: "POST" });
    },
  };
}
