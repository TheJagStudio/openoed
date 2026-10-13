import { HttpBackend, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, tap, throwError } from 'rxjs';
import type { AuthResponse, User } from './models';

const KEY = 'openoed.session';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly raw = new HttpClient(inject(HttpBackend));
  private readonly router = inject(Router);
  private readonly session = signal<AuthResponse | null>(read());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly accessToken = computed(() => this.session()?.accessToken ?? null);
  readonly signedIn = computed(() => this.user() !== null);

  login(email: string, password: string): Observable<User> {
    return this.raw.post<AuthResponse>('/api/v1/auth/login', { email, password }).pipe(tap((r) => this.save(r)), map((r) => r.user));
  }

  register(email: string, password: string, displayName: string): Observable<User> {
    return this.raw
      .post<AuthResponse>('/api/v1/auth/register', { email, password, displayName })
      .pipe(tap((r) => this.save(r)), map((r) => r.user));
  }

  refresh(): Observable<string> {
    const current = this.session();
    if (!current) return throwError(() => new Error('signed out'));
    return this.raw.post<AuthResponse>('/api/v1/auth/refresh', { refreshToken: current.refreshToken }).pipe(
      tap((r) => this.save(r)),
      map((r) => r.accessToken),
      catchError((err) => {
        this.clear();
        return throwError(() => err);
      }),
    );
  }

  logout(): void {
    const token = this.session()?.refreshToken;
    if (token) this.raw.post('/api/v1/auth/logout', { refreshToken: token }).pipe(catchError(() => of(null))).subscribe();
    this.clear();
    void this.router.navigateByUrl('/');
  }

  private save(response: AuthResponse): void {
    this.session.set(response);
    localStorage.setItem(KEY, JSON.stringify(response));
  }

  private clear(): void {
    this.session.set(null);
    localStorage.removeItem(KEY);
  }
}

function read(): AuthResponse | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  } catch {
    return null;
  }
}

export function problemMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const detail = error.error?.detail;
    if (typeof detail === 'string' && detail.length) return detail;
    if (error.status === 0) return 'The API is not reachable';
    return `Request failed (${error.status})`;
  }
  return 'Something went wrong';
}
