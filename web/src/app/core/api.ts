import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { ApplicationDetail, ApplicationSummary, AuditEvent, Dashboard } from './models';

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  stats() {
    return this.http.get<{ applications: number; priorityReviews: number; abandoned: number; notices: number }>('/api/v1/public/stats');
  }

  publicStatus(serial: string) {
    return this.http.get<{ serialNumber: string; mark: string; status: string; filedAt: string }>(`/api/v1/public/status/${serial}`);
  }

  dashboard() {
    return this.http.get<Dashboard>('/api/v1/dashboard');
  }

  applications(params: Record<string, string> = {}) {
    return this.http.get<ApplicationSummary[]>('/api/v1/applications', { params });
  }

  application(id: string) {
    return this.http.get<ApplicationDetail>(`/api/v1/applications/${id}`);
  }

  specimen(id: string) {
    return this.http.get(`/api/v1/applications/${id}/specimen`, { responseType: 'blob' });
  }

  file(body: unknown, specimen: File) {
    const data = new FormData();
    data.append('application', new Blob([JSON.stringify(body)], { type: 'application/json' }));
    data.append('specimen', specimen);
    return this.http.post<ApplicationDetail>('/api/v1/applications', data);
  }

  approve(id: string) {
    return this.http.post<ApplicationDetail>(`/api/v1/applications/${id}/approve`, {});
  }

  issue(id: string, type: string, body: string) {
    return this.http.post<ApplicationDetail>(`/api/v1/applications/${id}/office-actions`, { type, body });
  }

  respond(id: string, body: string) {
    return this.http.post<ApplicationDetail>(`/api/v1/office-actions/${id}/responses`, { body });
  }

  simulate(id: string) {
    return this.http.post<{ outcome: string }>(`/api/v1/office-actions/${id}/simulate-deadline`, {});
  }

  reviews(status = 'OPEN') {
    return this.http.get<{ reviewId: string; reviewStatus: string; priority: number; application: ApplicationSummary }[]>('/api/v1/reviews', { params: { status } });
  }

  decide(applicationId: string, decision: string, notes: string) {
    return this.http.post<ApplicationDetail>(`/api/v1/reviews/${applicationId}`, { decision, notes });
  }

  deadlines() {
    return this.http.get<{ id: string; applicationId: string; scheduleName: string; status: string; statutoryDueAt: string; scheduleFireAt: string }[]>('/api/v1/deadlines');
  }

  notices() {
    return this.http.get<{ toEmail: string; subject: string; body: string; providerMessageId?: string; sentAt: string; applicationId: string }[]>('/api/v1/notices');
  }

  audit(params: Record<string, string>) {
    return this.http.get<AuditEvent[]>('/api/v1/audit', { params });
  }

  system() {
    return this.http.get<Record<string, unknown>>('/api/v1/system');
  }

  shells() {
    return this.http.get<{ id: string; address: string; note?: string }[]>('/api/v1/admin/shell-addresses');
  }
}
