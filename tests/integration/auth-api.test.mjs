import assert from "node:assert/strict";
import test from "node:test";
import { createAuthApi } from "../../src/features/auth/auth-api.js";

test("认证 API 先通过 Gateway 获取验证码，再提交 RuoYi 所需字段", async () => {
  const requests = [];
  const authApi = createAuthApi({
    gatewayUrl: "https://gateway.example.com",
    fetchImpl: async (url, options) => {
      requests.push({ url, options });
      const isCaptcha = url.endsWith("/code");
      return {
        ok: true,
        json: async () => isCaptcha
          ? { code: 200, captchaEnabled: true, uuid: "captcha-1", img: "base64-image" }
          : { code: 0, data: { accessToken: "access" } },
      };
    },
  });

  const captcha = await authApi.getCaptcha();
  const response = await authApi.login({ username: "admin", password: "password", code: "4+2", uuid: captcha.uuid });

  assert.equal(captcha.uuid, "captcha-1");
  assert.equal(captcha.img, "base64-image");
  assert.deepEqual(response, { accessToken: "access" });
  assert.equal(requests[0].url, "https://gateway.example.com/code");
  assert.equal(requests[0].options.headers.Accept, "text/plain");
  assert.equal(requests[1].url, "https://gateway.example.com/auth/login");
  assert.equal(requests[1].options.body, '{"username":"admin","password":"password","code":"4+2","uuid":"captcha-1"}');
  assert.equal(requests[1].options.body.includes("tenantId"), false);
});

test("认证 API 使用 Gateway 会话令牌退出", async () => {
  let request;
  const authApi = createAuthApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access",
    fetchImpl: async (url, options) => {
      request = { url, options };
      return { ok: true, json: async () => ({ code: 0, data: null }) };
    },
  });

  await authApi.logout();
  assert.equal(request.url, "https://gateway.example.com/auth/logout");
  assert.equal(request.options.method, "DELETE");
  assert.equal(request.options.headers.Authorization, "Bearer access");
});

test("账号密码登录不发送验证码字段", async () => {
  let request;
  const authApi = createAuthApi({
    gatewayUrl: "https://gateway.example.com",
    fetchImpl: async (url, options) => {
      request = { url, options };
      return { ok: true, json: async () => ({ code: 0, data: { accessToken: "access" } }) };
    },
  });

  await authApi.login({ username: "admin", password: "admin123" });
  assert.equal(request.url, "https://gateway.example.com/auth/login");
  assert.equal(request.options.body, '{"username":"admin","password":"admin123"}');
});
