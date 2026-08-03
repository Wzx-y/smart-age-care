import assert from "node:assert/strict";
import test from "node:test";
import { createAuthApi } from "../../src/features/auth/auth-api.js";

test("认证 API 通过 Gateway 登录且不携带可伪造租户字段", async () => {
  let request;
  const authApi = createAuthApi({
    gatewayUrl: "https://gateway.example.com",
    fetchImpl: async (url, options) => {
      request = { url, options };
      return { ok: true, json: async () => ({ code: 0, data: { accessToken: "access" } }) };
    },
  });

  const response = await authApi.login({ account: "admin@example.com", password: "password" });
  assert.deepEqual(response, { accessToken: "access" });
  assert.equal(request.url, "https://gateway.example.com/auth/login");
  assert.equal(request.options.body, '{"account":"admin@example.com","password":"password"}');
  assert.equal(request.options.body.includes("tenantId"), false);
});
