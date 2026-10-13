import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ZardButtonComponent } from '@/shared/components/button';
import { AuthStore, problemMessage } from '../core/auth.store';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink, ZardButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="mx-auto flex min-h-dvh max-w-md flex-col justify-center px-6">
      <a routerLink="/" class="mb-8 font-semibold">OpenOED</a>
      <h1 class="text-2xl font-semibold">Create a free account</h1>
      <p class="text-muted-foreground mt-1 text-sm">New accounts file as applicants. An administrator can grant examiner, OED or inspector access.</p>
      <form class="mt-6 space-y-3" (ngSubmit)="submit()">
        <label class="field">Name <input name="name" [(ngModel)]="name" required /></label>
        <label class="field">Email <input name="email" type="email" [(ngModel)]="email" required /></label>
        <label class="field">Password <input name="password" type="password" [(ngModel)]="password" minlength="10" required /></label>
        @if (error()) { <p class="text-destructive text-sm">{{ error() }}</p> }
        <button z-button type="submit" [disabled]="busy()">Create account</button>
      </form>
      <p class="text-muted-foreground mt-6 text-sm">Already registered? <a routerLink="/login" class="text-primary underline">Sign in</a></p>
    </main>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  protected name = '';
  protected email = '';
  protected password = '';
  protected readonly busy = signal(false);
  protected readonly error = signal('');

  protected submit(): void {
    this.busy.set(true);
    this.auth.register(this.email, this.password, this.name).subscribe({
      next: () => void this.router.navigateByUrl('/app'),
      error: (err) => {
        this.error.set(problemMessage(err));
        this.busy.set(false);
      },
    });
  }
}
