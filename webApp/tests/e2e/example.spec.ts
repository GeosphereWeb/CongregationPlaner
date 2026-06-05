import { test, expect } from '@playwright/test';

test('load home page', async ({ page }) => {
  await page.goto('/');
  // Basic smoke check: page should load and have a body element
  await expect(page.locator('body')).toHaveCount(1);
});
