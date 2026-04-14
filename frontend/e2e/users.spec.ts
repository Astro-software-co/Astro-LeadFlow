import { expect, test } from "./helpers/fixtures";

test.describe("users - management", () => {
  test("creates and removes a vendor user", async ({ loginPage: page }) => {
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await page.getByRole("link", { name: /configurações|configuracoes/i }).click();
    await expect(page).toHaveURL(/\/app\/configuracoes/);
    await page.waitForLoadState("networkidle");

    const accessDenied = page.getByText(/acesso restrito/i);
    test.skip((await accessDenied.count()) > 0, "Current user is not allowed to manage users.");

    const timestamp = Date.now();
    const fullName = "Vendedor Teste";
    const email = `vendor.${timestamp}@astro.test`;

    await page.getByRole("button", { name: /adicionar usuário|adicionar usuario/i }).click();
    await expect(page.getByRole("heading", { name: /cadastrar usuário|cadastrar usuario/i })).toBeVisible();

    await page.getByLabel(/nome completo/i).fill(fullName);
    await page.getByLabel(/^e-mail \*$/i).fill(email);
    await page.getByLabel(/^senha \*$/i).fill("SenhaForte123");
    await page.getByLabel(/confirmar senha/i).fill("SenhaForte123");
    await page.getByRole("button", { name: /cadastrar usuário|cadastrar usuario/i }).click();

    const row = page.locator("tr", { hasText: email }).first();
    await expect(row).toBeVisible({ timeout: 15_000 });

    await row.locator("button").nth(1).click();
    await expect(page.locator("tr", { hasText: email })).toHaveCount(0);
  });
});
