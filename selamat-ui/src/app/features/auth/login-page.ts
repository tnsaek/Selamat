import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../core/services';
import { ErrorResponse } from '../../models';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly infoMessage = signal<string | null>(this.sessionMessage());

  readonly loginForm = this.formBuilder.nonNullable.group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.infoMessage.set(null);

    this.authService
      .login(this.loginForm.getRawValue())
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/feed'),
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  isInvalid(controlName: keyof typeof this.loginForm.controls): boolean {
    const control = this.loginForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  private errorText(error: HttpErrorResponse): string {
    const response = error.error as Partial<ErrorResponse> | undefined;
    if (response?.message) {
      return response.message;
    }

    if (error.status === 0) {
      return 'Cannot reach the backend. Check that the API is running and CORS is configured.';
    }

    if (error.status === 401) {
      return 'Invalid username, email, or password.';
    }

    return `Login failed with status ${error.status}.`;
  }

  private sessionMessage(): string | null {
    const reason = this.route.snapshot.queryParamMap.get('reason');

    if (reason === 'inactive') {
      return 'You were logged out because your session was inactive.';
    }

    if (reason === 'expired') {
      return 'Your session expired. Log in again to continue.';
    }

    return null;
  }
}
