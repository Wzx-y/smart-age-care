import assert from "node:assert/strict";
import test from "node:test";
import { createSessionStore } from "../../src/features/auth/session-store.js";

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
