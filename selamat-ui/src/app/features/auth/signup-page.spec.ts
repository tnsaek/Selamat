import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../core/services';
import { SignupPage } from './signup-page';

describe('SignupPage', () => {
  let fixture: ComponentFixture<SignupPage>;
  let authService: {
    signup: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();

    authService = {
      signup: vi.fn(() => of({ accessToken: 'access-token' })),
    };

    await TestBed.configureTestingModule({
      imports: [SignupPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    }).compileComponents();

    fixture = TestBed.createComponent(SignupPage);
    fixture.detectChanges();
  });

  it('shows validation messages and does not submit invalid form', () => {
    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(authService.signup).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Username must be between 3 and 50 characters.');
    expect(fixture.nativeElement.textContent).toContain('Enter a valid email address.');
    expect(fixture.nativeElement.textContent).toContain('Password must be between 8 and 100 characters.');
  });

  it('submits signup details and redirects to feed', () => {
    const router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture.componentInstance.signupForm.setValue({
      username: 'selam',
      email: 'selam@example.com',
      password: 'password123',
    });
    fixture.componentInstance.submit();

    expect(authService.signup).toHaveBeenCalledWith({
      username: 'selam',
      email: 'selam@example.com',
      password: 'password123',
    });
    expect(navigateByUrl).toHaveBeenCalledWith('/feed');
    expect(fixture.componentInstance.isSubmitting()).toBe(false);
  });

  it('shows backend error message when signup fails', () => {
    authService.signup.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'Username already exists.' } })),
    );

    fixture.componentInstance.signupForm.setValue({
      username: 'selam',
      email: 'selam@example.com',
      password: 'password123',
    });
    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Username already exists.');
    expect(fixture.componentInstance.isSubmitting()).toBe(false);
  });

  it('shows fallback error message when signup fails without backend message', () => {
    authService.signup.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    fixture.componentInstance.signupForm.setValue({
      username: 'selam',
      email: 'selam@example.com',
      password: 'password123',
    });
    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Signup failed. Check your details and try again.');
  });
});
