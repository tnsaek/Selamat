import { expect, test } from '@playwright/test';

test.describe('auth and routing smoke tests', () => {
  test('redirects unauthenticated users from protected pages to login', async ({ page }) => {
    await page.goto('/feed');

    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible();
  });

  test('shows the signup page and links back to login', async ({ page }) => {
    await page.goto('/signup');

    await expect(page.getByRole('heading', { name: 'Create account' })).toBeVisible();
    await expect(page.getByLabel('Username')).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('Password')).toBeVisible();

    await page.getByRole('link', { name: 'Log in' }).click();

    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible();
  });

  test('shows validation messages when login is submitted empty', async ({ page }) => {
    await page.goto('/login');

    await page.getByLabel('Username or email').focus();
    await page.getByLabel('Password').focus();
    await page.getByLabel('Password').blur();

    await expect(page.getByText('Username or email is required.')).toBeVisible();
    await expect(page.getByText('Password is required.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Log in' })).toBeDisabled();
  });

  test('shows forgot password page from login', async ({ page }) => {
    await page.goto('/login');

    await page.getByRole('link', { name: 'Forgot password?' }).click();

    await expect(page).toHaveURL(/\/forgot-password$/);
    await expect(page.getByRole('heading', { name: 'Reset password' })).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
  });
});
