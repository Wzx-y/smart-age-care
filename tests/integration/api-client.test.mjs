import assert from "node:assert/strict";
import test from "node:test";
import { ApiError, createApiClient } from "../../src/lib/api-client.js";

test("API 客户端传递令牌与幂等键，不传递可伪造 tenantId", async () => {
  let request;
  const client = createApiClient({
    baseUrl: "https://gateway.example.com/",
    getAccessToken: async () => "token",
    fetchImpl: async (url, options) => {
      request = { url, options };
      return { ok: true, json: async () => ({ code: 0, data: { id: "admission-1" } }) };
    },
  });

  const data = await client.request("/api/v1/admissions/admission-1/confirm", {
    method: "POST",
    body: { version: 3 },
    idempotencyKey: "request-1",
  });

  assert.deepEqual(data, { id: "admission-1" });
  assert.equal(request.url, "https://gateway.example.com/care/api/v1/admissions/admission-1/confirm");
  assert.equal(request.options.headers.Authorization, "Bearer token");
  assert.equal(request.options.headers["Idempotency-Key"], "request-1");
  assert.equal(request.options.body, '{"version":3}');
});

test("API 客户端保留追踪 ID 以支持云端排错", async () => {
  const client = createApiClient({
    baseUrl: "https://gateway.example.com",
    fetchImpl: async () => ({ ok: false, status: 403, json: async () => ({ code: 403, message: "权限不足", traceId: "trace-1" }) }),
  });

  await assert.rejects(
    () => client.request("/api/v1/residents/resident-1"),
    (error) => error instanceof ApiError && error.status === 403 && error.traceId === "trace-1",
  );
});
