import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ZardButtonComponent } from '@/shared/components/button';
import { AuthStore, problemMessage } from '../core/auth.store';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, ZardButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="mx-auto flex min-h-dvh max-w-md flex-col justify-center px-6">
      <a routerLink="/" class="mb-8 font-semibold">OpenOED</a>
      <h1 class="text-2xl font-semibold">Sign in</h1>
      <p class="text-muted-foreground mt-1 text-sm">Free accounts. Dev personas are on the sign-in form.</p>
      <form class="mt-6 space-y-3" (ngSubmit)="submit()">
        <label class="field">Email <input id="login-email" name="email" type="email" [(ngModel)]="email" required /></label>
        <label class="field">Password <input id="login-password" name="password" type="password" [(ngModel)]="password" required /></label>
        @if (error()) { <p class="text-destructive text-sm">{{ error() }}</p> }
        <button z-button type="submit" class="w-full" [disabled]="busy()">Sign in</button>
      </form>
      <div class="mt-4 flex flex-wrap gap-2 text-xs">
        @for (persona of personas; track persona.email) {
          <button type="button" class="bg-secondary rounded-full px-2 py-1" (click)="use(persona)">{{ persona.label }}</button>
        }
      </div>
      <p class="text-muted-foreground mt-6 text-sm">No account? <a routerLink="/register" class="text-primary underline">Create one</a></p>
    </main>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  protected email = 'oed@openoed.local';
  protected password = 'Oed#12345';
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly personas = [
    { label: 'OED', email: 'oed@openoed.local', password: 'Oed#12345' },
    { label: 'Examiner', email: 'examiner@openoed.local', password: 'Examiner#12345' },
    { label: 'Applicant', email: 'applicant@openoed.local', password: 'Applicant#12345' },
    { label: 'Inspector', email: 'ig@openoed.local', password: 'Inspector#12345' },
    { label: 'Admin', email: 'admin@openoed.local', password: 'Admin#12345' },
  ];

  protected use(persona: { email: string; password: string }): void {
    this.email = persona.email;
    this.password = persona.password;
  }

  protected submit(): void {
    this.busy.set(true);
    this.error.set('');
    this.auth.login(this.email, this.password).subscribe({
      next: () => void this.router.navigateByUrl('/app'),
      error: (err) => {
        this.error.set(problemMessage(err));
        this.busy.set(false);
      },
    });
  }
}
