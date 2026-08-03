import assert from "node:assert/strict";
import test from "node:test";
import { TenantScopeError, assertTenantMatch, hasAnyPermission } from "../../src/lib/tenant-access.js";

test("允许当前租户读取自身资源", () => {
  const resident = { id: "resident-1", tenantId: "tenant-a" };
  assert.equal(assertTenantMatch(resident, "tenant-a"), resident);
});

test("拒绝跨租户资源访问", () => {
  assert.throws(
    () => assertTenantMatch({ id: "resident-2", tenantId: "tenant-b" }, "tenant-a"),
    TenantScopeError,
  );
});

test("权限至少匹配一个所需权限", () => {
  assert.equal(hasAnyPermission(["resident:read"], ["resident:write", "resident:read"]), true);
  assert.equal(hasAnyPermission(["resident:read"], ["device:write"]), false);
});
