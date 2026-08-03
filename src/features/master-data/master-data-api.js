import { createApiClient } from "../../lib/api-client.js";

export function createMasterDataApi({ gatewayUrl, getAccessToken, getRequestHeaders, fetchImpl }) {
  const client = createApiClient({ baseUrl: `${gatewayUrl.replace(/\/$/, "")}/care`, getAccessToken, getRequestHeaders, fetchImpl });
  const suffix = (includeDisabled) => (includeDisabled ? "?includeDisabled=true" : "");

  return {
    listRooms({ includeDisabled = false } = {}) {
      return client.request(`/api/v1/master-data/rooms${suffix(includeDisabled)}`);
    },
    createRoom(payload) {
      return client.request("/api/v1/master-data/rooms", { method: "POST", body: payload });
    },
    updateRoom(roomId, payload) {
      return client.request(`/api/v1/master-data/rooms/${encodeURIComponent(roomId)}`, { method: "PATCH", body: payload });
    },
    changeRoomStatus(roomId, payload) {
      return client.request(`/api/v1/master-data/rooms/${encodeURIComponent(roomId)}/status`, { method: "POST", body: payload });
    },
    listBeds({ includeDisabled = false } = {}) {
      return client.request(`/api/v1/master-data/beds${suffix(includeDisabled)}`);
    },
    createBed(payload) {
      return client.request("/api/v1/master-data/beds", { method: "POST", body: payload });
    },
    updateBed(bedId, payload) {
      return client.request(`/api/v1/master-data/beds/${encodeURIComponent(bedId)}`, { method: "PATCH", body: payload });
    },
    changeBedStatus(bedId, payload) {
      return client.request(`/api/v1/master-data/beds/${encodeURIComponent(bedId)}/status`, { method: "POST", body: payload });
    },
    listCatalog(category, { includeDisabled = false } = {}) {
      return client.request(`/api/v1/master-data/catalogs/${encodeURIComponent(category)}${suffix(includeDisabled)}`);
    },
    createCatalogItem(category, payload) {
      return client.request(`/api/v1/master-data/catalogs/${encodeURIComponent(category)}`, { method: "POST", body: payload });
    },
    updateCatalogItem(category, itemId, payload) {
      return client.request(`/api/v1/master-data/catalogs/${encodeURIComponent(category)}/${encodeURIComponent(itemId)}`, { method: "PATCH", body: payload });
    },
    changeCatalogItemStatus(category, itemId, payload) {
      return client.request(`/api/v1/master-data/catalogs/${encodeURIComponent(category)}/${encodeURIComponent(itemId)}/status`, { method: "POST", body: payload });
    },
  };
}
