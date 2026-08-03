import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const gatewayProxyTarget = env.VITE_GATEWAY_PROXY_TARGET
    || process.env.CODESPACES_GATEWAY_URL
    || "http://127.0.0.1:8080";

  return {
    optimizeDeps: {
      include: ["react", "react-dom/client"],
    },
    server: {
      host: "0.0.0.0",
      allowedHosts: ["terminal.local", ".app.github.dev", ".githubpreview.dev"],
      proxy: {
        "/gateway": {
          target: gatewayProxyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/gateway/, ""),
        },
      },
      warmup: {
        clientFiles: ["./src/main.jsx"],
      },
    },
    plugins: [react()],
  };
});
