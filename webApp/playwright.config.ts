import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 30000,
  expect: { timeout: 5000 },
  fullyParallel: true,
  use: {
    baseURL: 'http://localhost:8080',
    headless: true,
    viewport: { width: 1280, height: 720 },
    actionTimeout: 0,
    ignoreHTTPSErrors: true
  },
  webServer: {
    // Use Gradle dev server for the webApp. From webApp folder the wrapper is at ../gradlew
    command: '../gradlew :webApp:wasmJsBrowserDevelopmentRun',
    url: 'http://localhost:8080',
    timeout: 120000,
    reuseExistingServer: false
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } }
  ]
});
