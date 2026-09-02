import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../core/services';
import { ErrorResponse } from '../../models';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password-page.html',
  styleUrl: './forgot-password-page.scss',
})
export class ForgotPasswordPage {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly forgotPasswordForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
  });

  submit(): void {
    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService
      .forgotPassword({
        email: this.forgotPasswordForm.getRawValue().email.trim(),
      })
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => this.successMessage.set(response.message),
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  isInvalid(controlName: keyof typeof this.forgotPasswordForm.controls): boolean {
    const control = this.forgotPasswordForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  private errorText(error: HttpErrorResponse): string {
    const response = error.error as Partial<ErrorResponse> | undefined;
    return response?.message ?? 'Unable to request password reset. Try again.';
  }
}
