import { existsSync, readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const moduleEntrypoints = [
  '../modules/crm/index.ts',
  '../modules/inventory/index.ts',
  '../modules/communication/index.ts',
  '../modules/saas/index.ts',
] as const;

const routePages = [
  '../pages/Dashboard.tsx',
  '../pages/Leads.tsx',
  '../pages/Kanban.tsx',
  '../pages/Estoque.tsx',
  '../pages/Comunicacao.tsx',
  '../pages/Configuracoes.tsx',
] as const;

const restrictedImports = [
  /from ['"]@\/hooks\/api\//,
  /from ['"]@\/components\/dashboard\//,
  /from ['"]@\/components\/kanban\//,
  /from ['"]@\/components\/settings\//,
  /from ['"]@\/features\/leads\//,
  /from ['"]@\/mappers\/leads\.mapper['"]/,
  /from ['"]@\/components\/modals\/AddVehicleModal['"]/,
];

describe('module boundaries', () => {
  it('keeps module entrypoints in place', () => {
    moduleEntrypoints.forEach((entrypoint) => {
      const fileUrl = new URL(entrypoint, import.meta.url);
      expect(existsSync(fileUrl)).toBe(true);
      const content = readFileSync(fileUrl, 'utf-8');
      expect(content).toContain('export');
    });
  });

  it('forces route pages to consume module APIs', () => {
    routePages.forEach((pagePath) => {
      const fileUrl = new URL(pagePath, import.meta.url);
      const content = readFileSync(fileUrl, 'utf-8');

      expect(content).toMatch(/from ['"]@\/modules\//);
      restrictedImports.forEach((restrictedPattern) => {
        expect(content).not.toMatch(restrictedPattern);
      });
    });
  });
});
