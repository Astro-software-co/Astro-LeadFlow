import { expect, test } from "./helpers/fixtures";
import { dragKanbanCard } from "./helpers/dragKanbanCard";

test.describe("kanban - interactions", () => {
  test("opens card detail modal and saves changes", async ({ loginPage: page }) => {
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await page.getByRole("link", { name: /painel de controle/i }).click();
    await expect(page).toHaveURL(/\/app\/kanban/);
    await page.waitForLoadState("networkidle");

    const firstCard = page.locator("[data-rfd-draggable-id]").first();
    test.skip((await firstCard.count()) === 0, "No kanban cards available in seed data.");

    await firstCard.click();
    await expect(page.getByText(/detalhes do lead/i)).toBeVisible();
    await page.getByLabel(/observações|observacoes/i).fill(`Nota E2E ${Date.now()}`);
    await page.getByRole("button", { name: /salvar alterações|salvar alteracoes/i }).click();
    await expect(page.getByText(/detalhes do lead/i)).not.toBeVisible();
  });

  test("moves a card to negotiation column using mouse events", async ({ loginPage: page }) => {
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await page.getByRole("link", { name: /painel de controle/i }).click();
    await expect(page).toHaveURL(/\/app\/kanban/);
    await page.waitForLoadState("networkidle");

    const firstCardTitle = page.locator("[data-rfd-draggable-id] h4").first();
    test.skip((await firstCardTitle.count()) === 0, "No kanban cards available in seed data.");

    const title = (await firstCardTitle.textContent())?.trim();
    test.skip(!title, "Could not read a card title.");

    await dragKanbanCard(page, title!, '[data-rfd-droppable-id="negociacao"]');

    const targetColumn = page.locator('[data-rfd-droppable-id="negociacao"]');
    await expect(targetColumn.getByText(title!)).toBeVisible();
  });

  test("assumes a waiting card when button is available", async ({ loginPage: page }) => {
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await page.getByRole("link", { name: /painel de controle/i }).click();
    await expect(page).toHaveURL(/\/app\/kanban/);
    await page.waitForLoadState("networkidle");

    const assumeButton = page.getByRole("button", { name: /assumir/i }).first();
    test.skip((await assumeButton.count()) === 0, "No unassigned waiting lead available to assume.");

    await assumeButton.click();
    await expect(page.getByText(/você assumiu este lead|voce assumiu este lead/i)).toBeVisible();
  });
});
