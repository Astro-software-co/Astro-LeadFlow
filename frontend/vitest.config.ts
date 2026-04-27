import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react-swc";
import path from "path";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    env: {
      VITE_USE_MOCKS: "false",
    },
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
    testTimeout: 15000,
    coverage: {
      provider: "v8",
      include: [
        "src/hooks/api/**/*.{ts,tsx}",
        "src/services/**/*.{ts,tsx}",
        "src/contexts/**/*.{ts,tsx}",
      ],
      exclude: [
        "src/**/*.d.ts",
        "src/test/**",
      ],
      reporter: ["text", "html", "json-summary"],
      thresholds: {
        lines: 80,
        functions: 80,
        branches: 80,
        statements: 80,
      },
    },
  },
  resolve: {
    alias: { "@": path.resolve(__dirname, "./src") },
  },
});
