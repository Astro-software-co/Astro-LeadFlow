import { expect, test } from "./helpers/fixtures";

test.describe("leads - core flow", () => {
  test("creates and edits a lead", async ({ loginPage: page }) => {
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await page.getByRole("link", { name: /^Leads$/i }).click();
    await expect(page).toHaveURL(/\/app\/leads/);
    await page.waitForLoadState("networkidle");

    const uniqueName = `E2E Lead ${Date.now()}`;
    const updatedNotes = `Atualizado em ${new Date().toISOString()}`;

    await page.getByRole("button", { name: /novo lead/i }).click();
    await expect(page.getByRole("heading", { name: /novo lead/i })).toBeVisible();
    await page.getByLabel(/^Nome$/).fill(uniqueName);
    await page.getByLabel(/^Telefone$/).fill("11999998888");
    await page.getByLabel(/e-mail/i).fill(`lead-${Date.now()}@mail.com`);
    await page.getByRole("button", { name: /criar lead/i }).click();

    await expect(page.getByText(uniqueName).first()).toBeVisible({ timeout: 15_000 });

    const row = page.locator("tr", { hasText: uniqueName }).first();
    await row.getByRole("button", { name: /editar/i }).click();
    await expect(page.getByRole("heading", { name: /editar lead/i })).toBeVisible();
    await page.getByLabel(/observacoes/i).fill(updatedNotes);
    await page.getByRole("button", { name: /salvar/i }).click();

    await expect(page.getByText(uniqueName).first()).toBeVisible();
  });
});
