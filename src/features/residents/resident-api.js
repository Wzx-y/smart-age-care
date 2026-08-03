import { createApiClient } from "../../lib/api-client.js";

export function createResidentApi({ gatewayUrl, getAccessToken, getRequestHeaders, fetchImpl }) {
  const client = createApiClient({ baseUrl: `${gatewayUrl.replace(/\/$/, "")}/care`, getAccessToken, getRequestHeaders, fetchImpl });

  return {
    listResidents(keyword) {
      const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
      return client.request(`/api/v1/residents${query}`);
    },
    getResident(residentId) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}`);
    },
    getResident360(residentId) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/360`);
    },
    createResident(payload) {
      return client.request("/api/v1/residents", { method: "POST", body: payload });
    },
    updateResident(residentId, payload) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}`, { method: "PATCH", body: payload });
    },
    getHealthProfile(residentId) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/health-profile`);
    },
    updateHealthProfile(residentId, payload) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/health-profile`, { method: "PATCH", body: payload });
    },
    archiveResident(residentId) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}`, { method: "DELETE" });
    },
    listContacts(residentId) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/contacts`);
    },
    createContact(residentId, payload) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/contacts`, { method: "POST", body: payload });
    },
    updateContact(residentId, contactId, payload) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/contacts/${encodeURIComponent(contactId)}`, { method: "PATCH", body: payload });
    },
    deleteContact(residentId, contactId, version) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/contacts/${encodeURIComponent(contactId)}?version=${encodeURIComponent(version)}`, { method: "DELETE" });
    },
    listAssessments(residentId) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/assessments`);
    },
    createAssessment(residentId, payload) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/assessments`, { method: "POST", body: payload });
    },
    updateAssessment(residentId, assessmentId, payload) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/assessments/${encodeURIComponent(assessmentId)}`, { method: "PATCH", body: payload });
    },
    deleteAssessment(residentId, assessmentId, version) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/assessments/${encodeURIComponent(assessmentId)}?version=${encodeURIComponent(version)}`, { method: "DELETE" });
    },
    listAttachments(residentId) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/attachments`);
    },
    prepareAttachment(residentId, payload) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/attachments`, { method: "POST", body: payload });
    },
    createAttachmentUploadTarget(residentId, attachmentId) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/attachments/${encodeURIComponent(attachmentId)}/upload-url`, { method: "POST" });
    },
    completeAttachmentUpload(residentId, attachmentId) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/attachments/${encodeURIComponent(attachmentId)}/complete`, { method: "POST" });
    },
    createAttachmentAccessTarget(residentId, attachmentId, inline = true) {
      return client.request(`/api/v1/residents/${encodeURIComponent(residentId)}/attachments/${encodeURIComponent(attachmentId)}/access-url?inline=${inline}`, { method: "POST" });
    },
  };
}
