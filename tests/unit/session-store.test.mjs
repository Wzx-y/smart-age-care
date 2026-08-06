import assert from "node:assert/strict";
import test from "node:test";
import { createSessionStore, isGatewaySession } from "../../src/features/auth/session-store.js";

test("会话仅保存在内存中且退出后清空", () => {
  const store = createSessionStore();
  assert.equal(store.get(), null);

  const input = { accessToken: "token", tenantId: "tenant-a" };
  const session = store.set(input);
  assert.equal(session.tenantId, "tenant-a");
  assert.notEqual(session, input);
  assert.equal(session, store.get());

  store.clear();
  assert.equal(store.get(), null);
});

test("演示会话不会启用 Gateway 业务请求", () => {
  assert.equal(isGatewaySession({ mode: "demo" }), false);
  assert.equal(isGatewaySession({ mode: "gateway" }), true);
  assert.equal(isGatewaySession(null), false);
});
