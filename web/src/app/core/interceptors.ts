import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { toast } from 'ngx-sonner';
import { AuthStore, problemMessage } from './auth.store';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStore);
  const token = auth.accessToken();
  const authed = token && !req.headers.has('Authorization') ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(authed).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && auth.accessToken() && !req.url.includes('/api/v1/auth/')) {
        return auth.refresh().pipe(switchMap((access) => next(req.clone({ setHeaders: { Authorization: `Bearer ${access}` } }))));
      }
      return throwError(() => error);
    }),
  );
};

export const errorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 && !req.url.includes('/api/v1/public/')) toast.error(problemMessage(error));
      return throwError(() => error);
    }),
  );
