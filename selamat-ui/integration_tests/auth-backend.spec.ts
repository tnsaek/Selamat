import { expect, test } from '@playwright/test';

test.describe('backend-connected authentication', () => {
  test('signs up through the UI, logs out, and logs back in', async ({ page }) => {
    const unique = Date.now().toString(36);
    const username = `e2euser${unique}`;
    const email = `${username}@example.com`;
    const password = 'StrongPassword123';

    await page.goto('/signup');

    await page.getByLabel('Username').fill(username);
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);

    const signupResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/auth/signup') && response.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Create account' }).click();
    const signupResponse = await signupResponsePromise;

    expect(signupResponse.status()).toBe(201);
    await expect(page).toHaveURL(/\/feed$/);
    await expect(page.getByRole('heading', { name: 'Feed' })).toBeVisible();

    const storedUserAfterSignup = await page.evaluate(() => localStorage.getItem('selamat.currentUser'));
    const accessTokenAfterSignup = await page.evaluate(() => localStorage.getItem('selamat.accessToken'));
    const refreshTokenAfterSignup = await page.evaluate(() => localStorage.getItem('selamat.refreshToken'));

    expect(storedUserAfterSignup).toContain(`"username":"${username}"`);
    expect(storedUserAfterSignup).toContain(`"email":"${email}"`);
    expect(accessTokenAfterSignup).toBeTruthy();
    expect(refreshTokenAfterSignup).toBeTruthy();

    const logoutResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/auth/logout') && response.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Logout' }).click();
    const logoutResponse = await logoutResponsePromise;

    expect(logoutResponse.status()).toBe(204);
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible();

    await page.getByLabel('Username or email').fill(email);
    await page.getByLabel('Password').fill(password);

    const loginResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/auth/login') && response.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Log in' }).click();
    const loginResponse = await loginResponsePromise;

    expect(loginResponse.status()).toBe(200);
    await expect(page).toHaveURL(/\/feed$/);
    await expect(page.getByRole('heading', { name: 'Feed' })).toBeVisible();

    const storedUserAfterLogin = await page.evaluate(() => localStorage.getItem('selamat.currentUser'));
    expect(storedUserAfterLogin).toContain(`"username":"${username}"`);
    expect(storedUserAfterLogin).toContain(`"email":"${email}"`);
  });

  test('shows backend validation for invalid login credentials', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel('Username or email').fill(`missing-${Date.now()}@example.com`);
    await page.getByLabel('Password').fill('WrongPassword123');

    const loginResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/api/auth/login') && response.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Log in' }).click();
    const loginResponse = await loginResponsePromise;

    expect(loginResponse.status()).toBe(401);
    await expect(page.getByRole('alert')).toContainText(/invalid username, email, or password/i);
    await expect(page).toHaveURL(/\/login$/);
  });
});
