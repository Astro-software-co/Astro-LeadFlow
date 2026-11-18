import { test as base, expect } from "@playwright/test";
import { test } from "./helpers/fixtures";

base.describe("auth - unauthenticated", () => {
  base("redirects /app/dashboard to /app/login when user is not authenticated", async ({ page }) => {
    await page.goto("/app/dashboard");
    await page.waitForURL(/\/app\/login/, { timeout: 10_000 });
    await expect(page).toHaveURL(/\/app\/login/);
  });

  base("renders login form fields and submit button", async ({ page }) => {
    await page.goto("/app/login");
    await expect(page.getByLabel(/e-?mail/i)).toBeVisible();
    await expect(page.getByLabel(/^senha$/i)).toBeVisible();
    await expect(page.getByRole("button", { name: /entrar/i })).toBeVisible();
  });
});

test.describe("auth - authenticated", () => {
  test("logs in through fixture and lands on dashboard", async ({ loginPage: page }) => {
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await expect(page.getByText(/vendas do mes/i)).toBeVisible();
  });

  test("keeps the user authenticated after a full page reload", async ({ loginPage: page }) => {
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await page.reload();
    await expect(page).toHaveURL(/\/app\/dashboard/);
    await expect(page.getByText(/vendas do mes/i)).toBeVisible();
  });
});
