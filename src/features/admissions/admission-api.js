import { createApiClient } from "../../lib/api-client.js";

export function createAdmissionApi({ gatewayUrl, getAccessToken, getRequestHeaders, fetchImpl, idempotencyKeyFactory = () => crypto.randomUUID() }) {
  const client = createApiClient({
    baseUrl: `${gatewayUrl.replace(/\/$/, "")}/care`,
    getAccessToken,
    getRequestHeaders,
    fetchImpl,
  });

  return {
    listAdmissions() {
      return client.request("/api/v1/admissions");
    },
    createAdmission(residentId, idempotencyKey = idempotencyKeyFactory()) {
      return client.request("/api/v1/admissions", { method: "POST", body: { residentId }, idempotencyKey });
    },
    listBeds(occupancyStatus) {
      const query = occupancyStatus ? `?occupancyStatus=${encodeURIComponent(occupancyStatus)}` : "";
      return client.request(`/api/v1/beds${query}`);
    },
    assignBed(admissionId, { bedId, admissionVersion, bedVersion }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/admissions/${encodeURIComponent(admissionId)}/assign-bed`, {
        method: "POST",
        body: { bedId, admissionVersion, bedVersion },
        idempotencyKey,
      });
    },
    decideAssessment(admissionId, { admissionVersion, decision, conclusion }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/admissions/${encodeURIComponent(admissionId)}/assessment`, {
        method: "POST",
        body: { admissionVersion, decision, conclusion },
        idempotencyKey,
      });
    },
    transferBed(admissionId, { targetBedId, admissionVersion, sourceBedVersion, targetBedVersion, reason }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/admissions/${encodeURIComponent(admissionId)}/transfer-bed`, {
        method: "POST",
        body: { targetBedId, admissionVersion, sourceBedVersion, targetBedVersion, reason },
        idempotencyKey,
      });
    },
    confirm(admissionId, { admissionVersion, bedVersion }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/admissions/${encodeURIComponent(admissionId)}/confirm`, {
        method: "POST",
        body: { admissionVersion, bedVersion },
        idempotencyKey,
      });
    },
    discharge(admissionId, { admissionVersion, bedVersion, dischargeReason }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/admissions/${encodeURIComponent(admissionId)}/discharge`, {
        method: "POST",
        body: { admissionVersion, bedVersion, dischargeReason },
        idempotencyKey,
      });
    },
    completeBedCleaning(bedId, { bedVersion, result }) {
      return client.request(`/api/v1/beds/${encodeURIComponent(bedId)}/complete-cleaning`, {
        method: "POST",
        body: { bedVersion, result },
      });
    },
    listAudit(admissionId) {
      return client.request(`/api/v1/admissions/${encodeURIComponent(admissionId)}/audit`);
    },
    cancel(admissionId, { admissionVersion, bedVersion }, idempotencyKey = idempotencyKeyFactory()) {
      return client.request(`/api/v1/admissions/${encodeURIComponent(admissionId)}/cancel`, {
        method: "POST",
        body: { admissionVersion, ...(bedVersion != null ? { bedVersion } : {}) },
        idempotencyKey,
      });
    },
  };
}
