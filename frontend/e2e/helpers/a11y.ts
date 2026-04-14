import AxeBuilder from "@axe-core/playwright";
import { expect, Page } from "@playwright/test";

export async function checkA11y(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page }).analyze();
  const criticalViolations = results.violations.filter((v) => v.impact === "critical");

  expect(
    criticalViolations,
    `Expected zero critical axe violations but found: ${criticalViolations.map((v) => v.id).join(", ")}`,
  ).toHaveLength(0);
}
