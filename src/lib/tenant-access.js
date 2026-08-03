export class TenantScopeError extends Error {
  constructor(message = "当前租户无权访问该资源") {
    super(message);
    this.name = "TenantScopeError";
  }
}

export function assertTenantMatch(resource, tenantId) {
  if (!tenantId || !resource?.tenantId || resource.tenantId !== tenantId) {
    throw new TenantScopeError();
  }
  return resource;
}

export function hasAnyPermission(grantedPermissions = [], requiredPermissions = []) {
  return requiredPermissions.some((permission) => grantedPermissions.includes(permission));
}
