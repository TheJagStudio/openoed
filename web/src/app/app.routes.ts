import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/landing.page').then((m) => m.LandingPage) },
  { path: 'login', loadComponent: () => import('./pages/login.page').then((m) => m.LoginPage) },
  { path: 'register', loadComponent: () => import('./pages/register.page').then((m) => m.RegisterPage) },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/shell.page').then((m) => m.ShellPage),
    children: [
      { path: '', loadComponent: () => import('./pages/dashboard.page').then((m) => m.DashboardPage) },
      { path: 'file', canActivate: [roleGuard('APPLICANT', 'ADMIN')], loadComponent: () => import('./pages/file.page').then((m) => m.FilePage) },
      { path: 'applications', loadComponent: () => import('./pages/applications.page').then((m) => m.ApplicationsPage) },
      { path: 'applications/:id', loadComponent: () => import('./pages/detail.page').then((m) => m.DetailPage) },
      { path: 'queue', canActivate: [roleGuard('OED', 'ADMIN')], loadComponent: () => import('./pages/queue.page').then((m) => m.QueuePage) },
      { path: 'deadlines', loadComponent: () => import('./pages/deadlines.page').then((m) => m.DeadlinesPage) },
      { path: 'inspector', canActivate: [roleGuard('IG', 'ADMIN', 'OED')], loadComponent: () => import('./pages/inspector.page').then((m) => m.InspectorPage) },
      { path: 'system', loadComponent: () => import('./pages/system.page').then((m) => m.SystemPage) },
    ],
  },
  { path: '**', redirectTo: '' },
];
