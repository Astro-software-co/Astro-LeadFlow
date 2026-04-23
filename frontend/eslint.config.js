import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["dist", "coverage", "playwright-report", "test-results"] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ["**/*.{ts,tsx}"],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      "react-refresh/only-export-components": ["warn", { allowConstantExport: true }],
      "@typescript-eslint/no-unused-vars": ["warn", { argsIgnorePattern: "^_", varsIgnorePattern: "^_" }],
    },
  },
  {
    files: ["src/pages/{Dashboard,Leads,Kanban,Estoque,Comunicacao,Configuracoes}.tsx"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: ["@/hooks/api/*"],
              message: "Route pages must consume module APIs from @/modules/*.",
            },
            {
              group: ["@/components/dashboard/*", "@/components/kanban/*", "@/components/settings/*"],
              message: "Import domain UI through module entrypoints under @/modules/*.",
            },
            {
              group: ["@/components/modals/AddVehicleModal", "@/features/leads/*", "@/mappers/leads.mapper"],
              message: "Do not import domain internals directly in route pages.",
            },
          ],
        },
      ],
    },
  },
);
