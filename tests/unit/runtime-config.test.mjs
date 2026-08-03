import assert from "node:assert/strict";
import test from "node:test";
import { resolveRuntimeConfig } from "../../src/lib/runtime-config.js";

test("未配置 Gateway 时强制使用演示认证模式", () => {
  const config = resolveRuntimeConfig({ VITE_AUTH_MODE: "gateway" });
  assert.equal(config.authMode, "demo");
  assert.equal(config.gatewayUrl, "");
});

test("Gateway 地址与认证模式同时配置后启用真实认证", () => {
  const config = resolveRuntimeConfig({
    VITE_APP_ENV: "preview",
    VITE_AUTH_MODE: "gateway",
    VITE_RUOYI_GATEWAY_URL: "https://api.example.com/",
  });
  assert.equal(config.appEnv, "preview");
  assert.equal(config.authMode, "gateway");
  assert.equal(config.gatewayUrl, "https://api.example.com");
});
