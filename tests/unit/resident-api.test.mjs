import test from "node:test";
import assert from "node:assert/strict";
import { createResidentApi } from "../../src/features/residents/resident-api.js";

test("resident API updates the authenticated tenant-scoped resident without a tenant field", async () => {
  let requested;
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com/",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 200, json: async () => ({ code: 0, data: { id: 11 } }) };
    },
  });

  await api.updateResident(11, {
    name: "李秀兰",
    gender: "女",
    birthDate: "1942-05-16",
    emergencyContactName: "李海",
    emergencyContactPhone: "13800138001",
  });

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/residents/11");
  assert.equal(requested.init.method, "PATCH");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
  assert.equal(requested.init.body.includes("tenantId"), false);
});

test("resident API creates contacts on the resident subresource", async () => {
  let requested;
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 201, json: async () => ({ code: 0, data: { id: 21 } }) };
    },
  });

  await api.createContact(11, {
    name: "王敏",
    relationshipText: "女儿",
    phone: "13800138001",
    primaryContact: true,
  });

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/residents/11/contacts");
  assert.equal(requested.init.method, "POST");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
  assert.equal(requested.init.body.includes("tenantId"), false);
});

test("resident API updates and deletes contacts with their current version", async () => {
  const requests = [];
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requests.push({ url, init });
      return { ok: true, status: 200, json: async () => ({ code: 0, data: {} }) };
    },
  });

  await api.updateContact(11, 21, { version: 3, name: "王敏", relationshipText: "女儿", phone: "13800138001", primaryContact: true });
  await api.deleteContact(11, 21, 4);

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/residents/11/contacts/21");
  assert.equal(requests[0].init.method, "PATCH");
  assert.equal(requests[0].init.body.includes("tenantId"), false);
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/residents/11/contacts/21?version=4");
  assert.equal(requests[1].init.method, "DELETE");
});

test("resident API creates assessment records on the resident subresource", async () => {
  let requested;
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 201, json: async () => ({ code: 0, data: { id: 31 } }) };
    },
  });

  await api.createAssessment(11, {
    assessmentType: "ADL",
    assessmentDate: "2026-07-31",
    score: 60,
    riskLevel: "MEDIUM",
    note: "需要协助洗浴",
  });

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/residents/11/assessments");
  assert.equal(requested.init.method, "POST");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
  assert.equal(requested.init.body.includes("tenantId"), false);
});

test("resident API updates and deletes assessments with their current version", async () => {
  const requests = [];
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requests.push({ url, init });
      return { ok: true, status: 200, json: async () => ({ code: 0, data: {} }) };
    },
  });

  await api.updateAssessment(11, 31, { version: 2, assessmentType: "ADL", assessmentDate: "2026-07-31", riskLevel: "HIGH", note: "跌倒风险" });
  await api.deleteAssessment(11, 31, 3);

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/residents/11/assessments/31");
  assert.equal(requests[0].init.method, "PATCH");
  assert.equal(requests[0].init.body.includes("tenantId"), false);
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/residents/11/assessments/31?version=3");
  assert.equal(requests[1].init.method, "DELETE");
});

test("resident API registers attachment metadata on the resident subresource", async () => {
  let requested;
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 201, json: async () => ({ code: 0, data: { id: 41 } }) };
    },
  });

  await api.prepareAttachment(11, {
    fileName: "assessment.pdf",
    contentType: "application/pdf",
    byteSize: 1024,
  });

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/residents/11/attachments");
  assert.equal(requested.init.method, "POST");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
  assert.equal(requested.init.body.includes("tenantId"), false);
});

test("resident API requests a signed upload target without exposing tenant scope", async () => {
  let requested;
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 200, json: async () => ({ code: 0, data: { uploadUrl: "https://storage.example/upload" } }) };
    },
  });

  await api.createAttachmentUploadTarget(11, 41);

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/residents/11/attachments/41/upload-url");
  assert.equal(requested.init.method, "POST");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
  assert.equal(requested.init.body, undefined);
});

test("resident API confirms an uploaded attachment through the authenticated gateway", async () => {
  let requested;
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 200, json: async () => ({ code: 0, data: { id: 41, uploadStatus: "UPLOADED" } }) };
    },
  });

  await api.completeAttachmentUpload(11, 41);

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/residents/11/attachments/41/complete");
  assert.equal(requested.init.method, "POST");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
  assert.equal(requested.init.body, undefined);
});

test("resident API keeps health records and private attachment access tenant-scoped", async () => {
  const requests = [];
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requests.push({ url, init });
      return { ok: true, status: 200, json: async () => ({ code: 0, data: {} }) };
    },
  });

  await api.updateHealthProfile(11, { careLevel: "LEVEL_2", fallRisk: "HIGH" });
  await api.createAttachmentAccessTarget(11, 41, true);

  assert.equal(requests[0].url, "https://gateway.example.com/care/api/v1/residents/11/health-profile");
  assert.equal(requests[0].init.method, "PATCH");
  assert.equal(requests[0].init.body.includes("tenantId"), false);
  assert.equal(requests[1].url, "https://gateway.example.com/care/api/v1/residents/11/attachments/41/access-url?inline=true");
  assert.equal(requests[1].init.headers.Authorization, "Bearer access-token");
});

test("resident API reads the 360 view only through the authenticated gateway", async () => {
  let requested;
  const api = createResidentApi({
    gatewayUrl: "https://gateway.example.com",
    getAccessToken: () => "access-token",
    fetchImpl: async (url, init) => {
      requested = { url, init };
      return { ok: true, status: 200, json: async () => ({ code: 0, data: {} }) };
    },
  });

  await api.getResident360(11);

  assert.equal(requested.url, "https://gateway.example.com/care/api/v1/residents/11/360");
  assert.equal(requested.init.headers.Authorization, "Bearer access-token");
});
