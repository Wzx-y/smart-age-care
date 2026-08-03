import assert from "node:assert/strict";
import test from "node:test";
import { createAdmissionApi } from "../../src/features/admissions/admission-api.js";

test("入住调度读取接口只通过 Gateway 和会话令牌确定租户范围", async () => {
  const requests = [];
  const admissionApi = createAdmissionApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: async () => "access-token",
    fetchImpl: async (url, options) => {
      requests.push({ url, options });
      return { ok: true, json: async () => ({ code: 0, data: [] }) };
    },
  });

  await admissionApi.listAdmissions();
  await admissionApi.listBeds("AVAILABLE");

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/admissions");
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/beds?occupancyStatus=AVAILABLE");
  assert.equal(requests[0].options.headers.Authorization, "Bearer access-token");
  assert.equal(requests[0].url.includes("tenantId"), false);
  assert.equal(requests[1].url.includes("tenantId"), false);
});

test("入住预留请求经 Gateway 发送版本号和幂等键，且不传递租户字段", async () => {
  let request;
  const admissionApi = createAdmissionApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: async () => "access-token",
    idempotencyKeyFactory: () => "assign-bed-1",
    fetchImpl: async (url, options) => {
      request = { url, options };
      return { ok: true, json: async () => ({ code: 0, data: { id: 12, status: "PENDING_CONFIRMATION" } }) };
    },
  });

  const response = await admissionApi.assignBed(12, { bedId: 71, admissionVersion: 3, bedVersion: 5 });

  assert.deepEqual(response, { id: 12, status: "PENDING_CONFIRMATION" });
  assert.equal(request.url, "https://gateway.example.com/care/api/v1/admissions/12/assign-bed");
  assert.equal(request.options.headers.Authorization, "Bearer access-token");
  assert.equal(request.options.headers["Idempotency-Key"], "assign-bed-1");
  assert.equal(request.options.body, '{"bedId":71,"admissionVersion":3,"bedVersion":5}');
  assert.equal(request.options.body.includes("tenantId"), false);
});

test("确认入住请求只发送当前版本与幂等键", async () => {
  let request;
  const admissionApi = createAdmissionApi({
    gatewayUrl: "https://gateway.example.com",
    idempotencyKeyFactory: () => "confirm-1",
    fetchImpl: async (url, options) => {
      request = { url, options };
      return { ok: true, json: async () => ({ code: 0, data: { id: 12, status: "ADMITTED" } }) };
    },
  });

  await admissionApi.confirm(12, { admissionVersion: 4, bedVersion: 6 });

  assert.equal(request.url, "https://gateway.example.com/care/api/v1/admissions/12/confirm");
  assert.equal(request.options.headers["Idempotency-Key"], "confirm-1");
  assert.equal(request.options.body, '{"admissionVersion":4,"bedVersion":6}');
  assert.equal(request.options.body.includes("tenantId"), false);
});

test("退住请求携带版本、原因和幂等键，且不传递租户字段", async () => {
  let request;
  const admissionApi = createAdmissionApi({
    gatewayUrl: "https://gateway.example.com",
    idempotencyKeyFactory: () => "discharge-1",
    fetchImpl: async (url, options) => {
      request = { url, options };
      return { ok: true, json: async () => ({ code: 0, data: { id: 12, status: "DISCHARGED" } }) };
    },
  });

  await admissionApi.discharge(12, {
    admissionVersion: 5,
    bedVersion: 7,
    dischargeReason: "转至家属照护",
  });

  assert.equal(request.url, "https://gateway.example.com/care/api/v1/admissions/12/discharge");
  assert.equal(request.options.headers["Idempotency-Key"], "discharge-1");
  assert.equal(request.options.body, '{"admissionVersion":5,"bedVersion":7,"dischargeReason":"转至家属照护"}');
  assert.equal(request.options.body.includes("tenantId"), false);
});

test("调用方可在网络重试时复用同一幂等键", async () => {
  const requestKeys = [];
  const admissionApi = createAdmissionApi({
    gatewayUrl: "https://gateway.example.com",
    idempotencyKeyFactory: () => "generated-key",
    fetchImpl: async (_url, options) => {
      requestKeys.push(options.headers["Idempotency-Key"]);
      return { ok: true, json: async () => ({ code: 0, data: { id: 12, status: "PENDING_CONFIRMATION" } }) };
    },
  });

  await admissionApi.assignBed(12, { bedId: 71, admissionVersion: 3, bedVersion: 5 }, "retry-key-1");
  await admissionApi.assignBed(12, { bedId: 71, admissionVersion: 3, bedVersion: 5 }, "retry-key-1");

  assert.deepEqual(requestKeys, ["retry-key-1", "retry-key-1"]);
});

test("创建和取消入住申请都通过 Gateway 传递最小参数与幂等键", async () => {
  const requests = [];
  const admissionApi = createAdmissionApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: async () => "access-token",
    idempotencyKeyFactory: () => "admission-write-1",
    fetchImpl: async (url, options) => {
      requests.push({ url, options });
      return { ok: true, json: async () => ({ code: 0, data: { id: 12 } }) };
    },
  });

  await admissionApi.createAdmission(20);
  await admissionApi.cancel(12, { admissionVersion: 4, bedVersion: 6 });

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/admissions");
  assert.equal(requests[0].options.headers["Idempotency-Key"], "admission-write-1");
  assert.equal(requests[0].options.body, '{"residentId":20}');
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/admissions/12/cancel");
  assert.equal(requests[1].options.headers.Authorization, "Bearer access-token");
  assert.equal(requests[1].options.body, '{"admissionVersion":4,"bedVersion":6}');
  assert.equal(requests.every((request) => request.options.body.includes("tenantId")), false);
});

test("评估决策、调床和清洁结果均通过 Gateway 发送版本与必要业务字段", async () => {
  const requests = [];
  const admissionApi = createAdmissionApi({
    gatewayUrl: "https://gateway.example.com",
    idempotencyKeyFactory: () => "admission-closure-1",
    fetchImpl: async (url, options) => {
      requests.push({ url, options });
      return { ok: true, json: async () => ({ code: 0, data: { id: 12 } }) };
    },
  });

  await admissionApi.decideAssessment(12, { admissionVersion: 1, decision: "PASSED", conclusion: "评估通过" });
  await admissionApi.transferBed(12, { targetBedId: 72, admissionVersion: 2, sourceBedVersion: 5, targetBedVersion: 3, reason: "照护距离调整" });
  await admissionApi.completeBedCleaning(72, { bedVersion: 4, result: "床单位及床旁已清洁消毒" });

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/admissions/12/assessment");
  assert.equal(requests[0].options.body, '{"admissionVersion":1,"decision":"PASSED","conclusion":"评估通过"}');
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/admissions/12/transfer-bed");
  assert.equal(requests[1].options.body.includes("tenantId"), false);
  assert.equal(requests[2].url, "https://gateway.example.com/care/api/v1/beds/72/complete-cleaning");
  assert.equal(requests[2].options.body, '{"bedVersion":4,"result":"床单位及床旁已清洁消毒"}');
});
