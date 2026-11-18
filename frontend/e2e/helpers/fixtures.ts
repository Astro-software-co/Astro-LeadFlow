import { expect, test as base, type APIRequestContext, type Page } from "@playwright/test";

const DEFAULT_E2E_PASSWORD = "SenhaForte123!";

type PipelineStage = "TALKING_TO_BOT" | "WAITING_ATTENDANCE" | "IN_ATTENDANCE" | "IN_NEGOTIATION" | "COMPLETED";

interface AuthCredentials {
  email: string;
  password: string;
}

interface LoginApiResponse {
  accessToken?: string;
  pendingTwoFactor?: boolean;
}

interface LeadApiResponse {
  id?: string;
  name?: string;
}

let cachedCredentials: AuthCredentials | null = null;

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

async function createIsolatedAdminUser(request: APIRequestContext): Promise<AuthCredentials> {
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

async function canAuthenticateApi(request: APIRequestContext, credentials: AuthCredentials): Promise<boolean> {
  const response = await request.post("/api/v1/auth/login", {
    data: credentials,
  });

  if (!response.ok()) {
    return false;
  }

  const payload = (await response.json()) as LoginApiResponse;
  return Boolean(payload.accessToken) && payload.pendingTwoFactor !== true;
}

async function resolveCredentials(request: APIRequestContext): Promise<AuthCredentials> {
  if (cachedCredentials) {
    return cachedCredentials;
  }

  const envEmail = process.env.E2E_ADMIN_EMAIL;
  const envPassword = process.env.E2E_ADMIN_PASSWORD;

  if (envEmail && envPassword) {
    const envCredentials = { email: envEmail, password: envPassword };
    if (await canAuthenticateApi(request, envCredentials)) {
      cachedCredentials = envCredentials;
      return cachedCredentials;
    }
  }

  return createIsolatedAdminUser(request);
}

export async function getAuthAccessToken(request: APIRequestContext): Promise<string> {
  const credentials = await resolveCredentials(request);
  const loginResponse = await request.post("/api/v1/auth/login", {
    data: credentials,
  });

  if (!loginResponse.ok()) {
    throw new Error(`Failed to authenticate seeded user: HTTP ${loginResponse.status()}`);
  }

  const payload = (await loginResponse.json()) as LoginApiResponse;
  if (!payload.accessToken || payload.pendingTwoFactor) {
    throw new Error("E2E seed user requires a direct accessToken without pending 2FA");
  }

  return payload.accessToken;
}

export interface SeedLeadOptions {
  name?: string;
  origin?: string;
  stage?: PipelineStage;
}

export async function seedLead(request: APIRequestContext, options: SeedLeadOptions = {}): Promise<{ id: string; name: string }> {
  const token = await getAuthAccessToken(request);
  const timestamp = Date.now();
  const leadName = options.name ?? `E2E Kanban Lead ${timestamp}`;

  const createResponse = await request.post("/api/v1/leads", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    data: {
      name: leadName,
      phoneNumber: `1199${String(timestamp).slice(-7)}`,
      email: `lead.${timestamp}@astro.test`,
      origin: options.origin ?? "Direto",
      vehicleInterest: "Civic Touring",
      notes: "Seeded automatically for Playwright preconditions",
    },
  });

  if (!createResponse.ok()) {
    throw new Error(`Failed to seed lead: HTTP ${createResponse.status()}`);
  }

  const created = (await createResponse.json()) as LeadApiResponse;
  if (!created.id) {
    throw new Error("Lead API did not return an id for seeded lead");
  }

  if (options.stage) {
    const moveResponse = await request.patch(`/api/v1/leads/${created.id}/move`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      data: {
        toStage: options.stage,
      },
    });

    if (!moveResponse.ok()) {
      throw new Error(`Failed to move seeded lead to stage ${options.stage}: HTTP ${moveResponse.status()}`);
    }
  }

  return { id: created.id, name: created.name ?? leadName };
}

export const test = base.extend<{ loginPage: Page }>({
  loginPage: async ({ page }, runFixture) => {
    const credentials = await resolveCredentials(page.request);
    const loginOk = await loginThroughUi(page, credentials.email, credentials.password);

    if (!loginOk) {
      const fallbackCredentials = await createIsolatedAdminUser(page.request);
      const fallbackLoginOk = await loginThroughUi(page, fallbackCredentials.email, fallbackCredentials.password);

      if (!fallbackLoginOk) {
        throw new Error("Fixture failed to authenticate test user via UI login");
      }
    }

    await expect(page).toHaveURL(/\/app\/dashboard/);
    await runFixture(page);
  },
});

export { expect } from "@playwright/test";
