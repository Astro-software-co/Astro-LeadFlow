import { test as base } from '@playwright/test';
import { expect, test } from './helpers/fixtures';
import { checkA11y } from './helpers/a11y';

base.describe('a11y - public', () => {
  base('login page has no critical accessibility violations', async ({ page }) => {
    await page.goto('/app/login');
    await checkA11y(page);
  });
});

test.describe('a11y - authenticated', () => {
  test('dashboard has no critical accessibility violations', async ({ loginPage: page }) => {
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await checkA11y(page);
  });

  test('leads has no critical accessibility violations', async ({ loginPage: page }) => {
    await page.getByRole('link', { name: /^Leads$/i }).click();
    await expect(page).toHaveURL(/\/app\/leads/);
    await checkA11y(page);
  });

  test('kanban has no critical accessibility violations', async ({ loginPage: page }) => {
    await page.getByRole('link', { name: /kanban/i }).click();
    await expect(page).toHaveURL(/\/app\/kanban/);
    await checkA11y(page);
  });

  test('estoque has no critical accessibility violations', async ({ loginPage: page }) => {
    await page.getByRole('link', { name: /^Estoque$/i }).click();
    await expect(page).toHaveURL(/\/app\/estoque/);
    await checkA11y(page);
  });

  test('configuracoes has no critical accessibility violations', async ({ loginPage: page }) => {
    await page.getByRole('link', { name: /configurações|configuracoes/i }).click();
    await expect(page).toHaveURL(/\/app\/configuracoes/);
    await checkA11y(page);
  });
});
