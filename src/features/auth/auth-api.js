import { createApiClient } from "../../lib/api-client.js";

export function createAuthApi({ gatewayUrl, getAccessToken, fetchImpl }) {
  const client = createApiClient({
    baseUrl: gatewayUrl,
    getAccessToken,
    fetchImpl,
  });

  return {
    getCaptcha() {
      return client.request("/code", {
        headers: { Accept: "text/plain" },
        responseType: "envelope",
      });
    },
    login({ username, password, code, uuid }) {
      return client.request("/auth/login", {
        method: "POST",
        body: { username, password, code, uuid },
      });
    },
    refresh(refreshToken) {
      return client.request("/auth/refresh", {
        method: "POST",
        body: { refreshToken },
      });
    },
    logout() {
      return client.request("/auth/logout", { method: "DELETE" });
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
