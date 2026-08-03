import { createApiClient } from "../../lib/api-client.js";

function unwrapRows(payload) {
  return payload.rows || payload.data || [];
}

export function createSystemApi({ gatewayUrl, getAccessToken, getRequestHeaders, fetchImpl }) {
  const client = createApiClient({ baseUrl: gatewayUrl, getAccessToken, getRequestHeaders, fetchImpl });

  return {
    async listTenantDirectory() {
      return unwrapRows(await client.request("/system/tenant-directory", { responseType: "envelope" }));
    },
    createTenant(payload) {
      return client.request("/system/tenant-directory", { method: "POST", body: payload });
    },
    updateTenant(tenantId, payload) {
      return client.request(`/system/tenant-directory/${encodeURIComponent(tenantId)}`, { method: "PUT", body: payload });
    },
    disableTenant(tenantId, version) {
      return client.request(`/system/tenant-directory/${encodeURIComponent(tenantId)}`, { method: "DELETE", body: { version } });
    },
    async listMembers() {
      const payload = await client.request("/system/tenant-members", { responseType: "envelope" });
      return payload.data || [];
    },
    async listRoles() {
      return unwrapRows(await client.request("/system/role/list", { responseType: "envelope" }));
    },
    async listUsers() {
      return unwrapRows(await client.request("/system/user/list", { responseType: "envelope" }));
    },
    async listDepartments() {
      return unwrapRows(await client.request("/system/dept/list", { responseType: "envelope" }));
    },
    addMember({ userId, roleIds }) {
      return client.request("/system/tenant-members", { method: "POST", body: { userId, roleIds } });
    },
    changeMemberStatus(userId, status) {
      return client.request(`/system/tenant-members/${encodeURIComponent(userId)}/status`, { method: "PUT", body: { status } });
    },
    replaceMemberRoles(userId, roleIds) {
      return client.request(`/system/tenant-members/${encodeURIComponent(userId)}/roles`, { method: "PUT", body: { roleIds } });
    },
    getRoleMenuAccess(roleId) {
      return client.request(`/system/menu/roleMenuTreeselect/${encodeURIComponent(roleId)}`);
    },
    updateRoleDataScope(role) {
      return client.request("/system/role/dataScope", {
        method: "PUT",
        body: { roleId: role.roleId, dataScope: role.dataScope, deptIds: role.deptIds || [] },
      });
    },
  };
}
