import { expect, Page } from "@playwright/test";

export async function dragKanbanCard(
  page: Page,
  cardMatcher: string,
  targetColumnLocator: string,
): Promise<void> {
  const card = page.locator("div").filter({ hasText: cardMatcher }).first();
  const targetColumn = page.locator(targetColumnLocator).first();

  await expect(card).toBeVisible();
  await expect(targetColumn).toBeVisible();

  const sourceBox = await card.boundingBox();
  const targetBox = await targetColumn.boundingBox();

  if (!sourceBox || !targetBox) {
    throw new Error("Could not get source/target bounding boxes for drag operation.");
  }

  const fromX = sourceBox.x + sourceBox.width / 2;
  const fromY = sourceBox.y + sourceBox.height / 2;
  const toX = targetBox.x + targetBox.width / 2;
  const toY = targetBox.y + Math.min(80, targetBox.height / 2);

  await page.mouse.move(fromX, fromY);
  await page.mouse.down();
  await page.mouse.move((fromX + toX) / 2, (fromY + toY) / 2, { steps: 12 });
  await page.mouse.move(toX, toY, { steps: 18 });
  await page.mouse.up();
}
