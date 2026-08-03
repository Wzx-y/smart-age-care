export class ApiError extends Error {
  constructor(message, { status, traceId } = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.traceId = traceId;
  }
}

export function createApiClient({ baseUrl, getAccessToken, getRequestHeaders, fetchImpl = fetch }) {
  const normalizedBaseUrl = baseUrl.replace(/\/$/, "");

  return {
    async request(path, { method = "GET", body, headers = {}, idempotencyKey, responseType = "data" } = {}) {
      const accessToken = await getAccessToken?.();
      const requestHeaders = await getRequestHeaders?.();
      const response = await fetchImpl(`${normalizedBaseUrl}${path}`, {
        method,
        headers: {
          Accept: "application/json",
          ...(body ? { "Content-Type": "application/json" } : {}),
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
          ...(requestHeaders || {}),
          ...(idempotencyKey ? { "Idempotency-Key": idempotencyKey } : {}),
          ...headers,
        },
        ...(body ? { body: JSON.stringify(body) } : {}),
      });
      const payload = await response.json();
      if (!response.ok || (payload.code !== 0 && payload.code !== 200)) {
        throw new ApiError(payload.message || payload.msg || "请求未完成", {
          status: response.status,
          traceId: payload.traceId,
        });
      }
      return responseType === "envelope" ? payload : payload.data;
    },
  };
}
