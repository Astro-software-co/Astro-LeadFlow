import { expect, type Page } from '@playwright/test';

/**
 * Helper to drag a kanban card from its current column to a target column element.
 * 
 * @param page The Playwright page instance
 * @param cardMatcher Text or Regexp to match the card (e.g., the title)
 * @param targetColumnSelector CSS selector for the drop zone (e.g., [data-rfd-droppable-id="negociacao"])
 */
export async function dragKanbanCard(page: Page, cardMatcher: string | RegExp, targetColumnSelector: string) {
  const card = page.locator('[data-rfd-draggable-id]', { hasText: cardMatcher }).first();
  const targetCol = page.locator(targetColumnSelector).first();

  await expect(card).toBeVisible();
  await expect(targetCol).toBeVisible();

  const cardBox = await card.boundingBox();
  const targetBox = await targetCol.boundingBox();

  if (!cardBox || !targetBox) {
    throw new Error('Could not find bounding box for card or target column');
  }

  // Move to the center of the card, hold, move to target, and release
  await page.mouse.move(cardBox.x + cardBox.width / 2, cardBox.y + cardBox.height / 2);
  await page.mouse.down();
  
  // Dragging sometimes requires a small movement first to trigger the start of DnD
  await page.mouse.move(cardBox.x + cardBox.width / 2 + 10, cardBox.y + cardBox.height / 2 + 10, { steps: 5 });
  
  // Move to target
  await page.mouse.move(targetBox.x + targetBox.width / 2, targetBox.y + targetBox.height / 2, { steps: 10 });
  await page.mouse.up();
}
