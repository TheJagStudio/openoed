import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDarkMode } from '@/shared/services/dark-mode';
import { AuthStore } from '../core/auth.store';
import { ROLE_LABEL, type Role } from '../core/models';

interface NavItem {
  path: string;
  label: string;
  roles?: Role[];
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ZardButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-dvh lg:grid lg:grid-cols-[240px_1fr]">
      <aside class="bg-sidebar text-sidebar-foreground flex flex-col gap-1 p-4 lg:min-h-dvh">
        <a routerLink="/app" class="mb-4 flex items-center gap-2 px-2 font-semibold">
          <span class="bg-sidebar-primary text-sidebar-primary-foreground grid size-8 place-items-center rounded-lg text-xs">OE</span>
          OpenOED
        </a>
        @for (item of visible(); track item.path) {
          <a
            [routerLink]="item.path"
            routerLinkActive="bg-sidebar-accent"
            [routerLinkActiveOptions]="{ exact: item.path === '/app' }"
            class="hover:bg-sidebar-accent rounded-lg px-3 py-2 text-sm"
            >{{ item.label }}</a
          >
        }
        <div class="mt-auto space-y-2 px-2 pt-6 text-xs opacity-80">
          <p>{{ auth.user()?.displayName }}</p>
          <p>{{ role() }}</p>
          <button z-button zType="secondary" zSize="sm" type="button" (click)="theme.toggleTheme()">Theme</button>
          <button z-button zType="ghost" zSize="sm" type="button" class="text-sidebar-foreground" (click)="auth.logout()">Sign out</button>
        </div>
      </aside>
      <main class="min-w-0 px-4 py-6 sm:px-8">
        <router-outlet />
      </main>
    </div>
  `,
})
export class ShellPage {
  protected readonly auth = inject(AuthStore);
  protected readonly theme = inject(ZardDarkMode);
  private readonly items: NavItem[] = [
    { path: '/app', label: 'Dashboard' },
    { path: '/app/file', label: 'File application', roles: ['APPLICANT', 'ADMIN'] },
    { path: '/app/applications', label: 'Applications' },
    { path: '/app/queue', label: 'Priority queue', roles: ['OED', 'ADMIN'] },
    { path: '/app/deadlines', label: 'Deadlines' },
    { path: '/app/inspector', label: 'Inspector General', roles: ['IG', 'ADMIN', 'OED'] },
    { path: '/app/system', label: 'System' },
  ];

  protected visible() {
    const role = this.auth.user()?.role;
    return this.items.filter((item) => !item.roles || (role && item.roles.includes(role)));
  }

  protected role(): string {
    const user = this.auth.user();
    if (!user) return '';
    return ROLE_LABEL[user.role] + (user.artUnit ? ` · ${user.artUnit}` : '');
  }
}
