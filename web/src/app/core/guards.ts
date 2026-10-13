import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './auth.store';
import type { Role } from './models';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  return auth.signedIn() ? true : inject(Router).createUrlTree(['/login']);
};

export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthStore);
    const role = auth.user()?.role;
    if (!role) return inject(Router).createUrlTree(['/login']);
    return roles.includes(role) ? true : inject(Router).createUrlTree(['/app']);
  };
}
