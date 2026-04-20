import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";

function resolveProxyTarget(apiUrl?: string) {
  if (!apiUrl || apiUrl.startsWith("/")) {
    return "http://localhost:8080";
  }

  return new URL(apiUrl).origin;
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  return {
    server: {
      host: "localhost",
      port: 3000,
      proxy: {
        "/api": {
          target: resolveProxyTarget(env.VITE_API_URL),
          changeOrigin: true,
        },
      },
    },
    plugins: [react()],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
  };
});
