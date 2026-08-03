function normalizeBaseUrl(value) {
  return typeof value === "string" ? value.trim().replace(/\/+$/, "") : "";
}

export function resolveRuntimeConfig(env = {}) {
  const gatewayUrl = normalizeBaseUrl(env.VITE_RUOYI_GATEWAY_URL);
  const requestedAuthMode = env.VITE_AUTH_MODE;
  const authMode = requestedAuthMode === "gateway" && gatewayUrl ? "gateway" : "demo";

  return Object.freeze({
    appEnv: env.VITE_APP_ENV || "development",
    authMode,
    gatewayUrl,
  });
}

export const runtimeConfig = resolveRuntimeConfig(import.meta.env);
