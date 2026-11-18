import { expect, test } from "./helpers/fixtures";

test.describe("vehicles - inventory flow", () => {
  test("creates and edits a vehicle", async ({ loginPage: page }) => {
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await page.getByRole("link", { name: /^Estoque$/i }).click();
    await expect(page).toHaveURL(/\/app\/estoque/);
    await page.waitForLoadState("networkidle");

    const uniqueModel = `E2E-${Date.now()}`;

    await page.getByRole("button", { name: /adicionar veiculo/i }).first().click();
    await expect(page.getByRole("heading", { name: /cadastrar veículo|cadastrar veiculo/i })).toBeVisible();

    await page.getByLabel(/^Marca \*$/i).fill("Toyota");
    await page.getByLabel(/^Modelo \*$/i).fill(uniqueModel);
    await page.getByLabel(/^Ano \*$/i).fill("2024");
    await page.getByLabel(/preço de venda|preco de venda/i).fill("150000");
    await page.getByLabel(/preço de custo|preco de custo/i).fill("120000");
    await page.getByLabel(/quilometragem/i).fill("25000");
    await page.getByRole("button", { name: /cadastrar veículo|cadastrar veiculo/i }).click();

    await expect(page.getByText(uniqueModel).first()).toBeVisible({ timeout: 15_000 });

    const row = page.locator("tr", { hasText: uniqueModel }).first();
    await row.getByRole("button", { name: /editar/i }).click();
    await expect(page.getByText(/editar veículo|editar veiculo/i)).toBeVisible();

    await page.getByLabel(/^Cor$/i).fill("Preto");
    await page.getByRole("button", { name: /salvar alterações|salvar alteracoes/i }).click();

    await expect(page.getByText(uniqueModel).first()).toBeVisible();
  });
});
