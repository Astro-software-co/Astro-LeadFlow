import { expect, seedLead, test } from './helpers/fixtures';
import type { Page } from '@playwright/test';
import type { SeedLeadOptions } from './helpers/fixtures';
import { dragKanbanCard } from './helpers/dragKanbanCard';

async function openKanban(page: Page) {
  await expect(page).toHaveURL(/\/app\/dashboard/);
  await page.getByRole('link', { name: /kanban/i }).click();
  await expect(page).toHaveURL(/\/app\/kanban/);
  await page.waitForLoadState('networkidle');
}

async function seedAndOpenKanban(page: Page, options?: SeedLeadOptions) {
  const seeded = await seedLead(page.request, options);
  await openKanban(page);
  await expect(page.getByRole('heading', { name: seeded.name, level: 4 }).first()).toBeVisible({ timeout: 15_000 });
  return seeded;
}

test.describe('kanban - interactions', () => {
  test('opens card detail modal and saves changes', async ({ loginPage: page }) => {
    const seeded = await seedAndOpenKanban(page);

    await page.getByRole('heading', { name: seeded.name, level: 4 }).first().click();

    const dialog = page.getByRole('dialog');
    await expect(dialog.getByText(/detalhes do lead/i)).toBeVisible();

    const saveButton = dialog.getByRole('button', { name: /salvar alterações|salvar alteracoes/i });
    await dialog.locator('#nome').fill(`${seeded.name} atualizado`);
    await dialog.locator('#telefone').fill('11999999999');
    await dialog.locator('#orcamento').fill('100000');
    await expect(saveButton).toBeEnabled();

    await saveButton.click();
    await expect(dialog).not.toBeVisible({ timeout: 10_000 });
  });

  test('moves a card to negotiation column using mouse events', async ({ loginPage: page }) => {
    const seeded = await seedAndOpenKanban(page);

    await dragKanbanCard(page, seeded.name, '[data-rfd-droppable-id="negociacao"]');

    const targetColumn = page.locator('[data-rfd-droppable-id="negociacao"]');
    await expect(targetColumn.getByText(seeded.name).first()).toBeVisible({ timeout: 15_000 });
  });

  test('assumes a waiting card when button is available', async ({ loginPage: page }) => {
    const seeded = await seedAndOpenKanban(page, {
      stage: 'WAITING_ATTENDANCE',
      name: `E2E Waiting ${Date.now()}`,
    });

    const card = page.locator('[data-rfd-draggable-id]', {
      has: page.getByRole('heading', { name: seeded.name, level: 4 }),
    }).first();

    await expect(card).toBeVisible({ timeout: 15_000 });

    const assumeButton = card.getByRole('button', { name: /assumir/i });
    await expect(assumeButton).toBeVisible({ timeout: 15_000 });

    await assumeButton.click();
    await expect(page.getByText(/você assumiu este lead|voce assumiu este lead/i)).toBeVisible();
  });
});
