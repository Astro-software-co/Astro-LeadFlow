import { expect, test as base, type APIRequestContext, type Page } from "@playwright/test";

const DEFAULT_E2E_PASSWORD = "SenhaForte123!";

let cachedCredentials: { email: string; password: string } | null = null;

async function loginThroughUi(page: Page, email: string, password: string): Promise<boolean> {
  await page.goto("/app/login");
  await page.getByLabel(/e-?mail/i).fill(email);
  await page.getByLabel(/^senha$/i).fill(password);
  await page.getByRole("button", { name: /entrar/i }).click();

  try {
    await page.waitForURL("**/app/dashboard", { timeout: 15_000 });
    return true;
  } catch {
    return false;
  }
}

async function createIsolatedAdminUser(request: APIRequestContext): Promise<{ email: string; password: string }> {
  if (cachedCredentials) {
    return cachedCredentials;
  }

  const timestamp = Date.now();
  const email = `admin.e2e.${timestamp}@astro.test`;
  const password = DEFAULT_E2E_PASSWORD;

  const response = await request.post("/api/v1/auth/register", {
    data: {
      organizationName: `Astro E2E ${timestamp}`,
      slug: `astro-e2e-${timestamp}`,
      adminName: "Admin E2E",
      adminEmail: email,
      password,
    },
  });

  if (!response.ok()) {
    throw new Error(`Failed to create E2E admin user: HTTP ${response.status()}`);
  }

  cachedCredentials = { email, password };
  return cachedCredentials;
}

export const test = base.extend<{ loginPage: Page }>({
  loginPage: async ({ page }, use) => {
    const envEmail = process.env.E2E_ADMIN_EMAIL;
    const envPassword = process.env.E2E_ADMIN_PASSWORD;

    if (envEmail && envPassword) {
      const envLoginOk = await loginThroughUi(page, envEmail, envPassword);
      if (envLoginOk) {
        await expect(page).toHaveURL(/\/app\/dashboard/);
        await use(page);
        return;
      }
    }

    const credentials = await createIsolatedAdminUser(page.request);
    const fallbackLoginOk = await loginThroughUi(page, credentials.email, credentials.password);

    if (!fallbackLoginOk) {
      throw new Error("Fixture failed to authenticate test user via UI login");
    }

    await expect(page).toHaveURL(/\/app\/dashboard/);
    await use(page);
  },
});

export { expect } from "@playwright/test";
