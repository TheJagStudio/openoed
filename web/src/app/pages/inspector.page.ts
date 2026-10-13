import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ZardButtonComponent } from '@/shared/components/button';
import { Api } from '../core/api';
import { isAnomaly, type AuditEvent } from '../core/models';

@Component({
  selector: 'app-inspector',
  imports: [DatePipe, FormsModule, ZardButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">Inspector General</h1>
    <p class="text-muted-foreground mt-1 max-w-3xl text-sm">
      Append-only ledger. Deletes are rejected. An examiner who opens a filing outside their art unit is flagged ART_UNIT_MISMATCH.
    </p>
    <form class="mt-4 flex flex-wrap gap-2" (ngSubmit)="load()">
      <input class="bg-card h-9 rounded-md border px-3 text-sm" placeholder="Action" [(ngModel)]="action" name="action" />
      <input class="bg-card h-9 rounded-md border px-3 text-sm" placeholder="Actor email" [(ngModel)]="actor" name="actor" />
      <label class="flex items-center gap-2 text-sm"><input type="checkbox" [(ngModel)]="anomaliesOnly" name="anomaly" /> Anomalies only</label>
      <button z-button zType="outline" type="submit">Query ledger</button>
    </form>
    <p class="mt-4 text-sm">{{ anomalyCount() }} anomalous events in this result.</p>
    <div class="bg-card mt-3 overflow-x-auto rounded-xl border">
      <table class="w-full text-left text-sm">
        <thead class="bg-muted/60"><tr><th class="px-3 py-2">When</th><th>Actor</th><th>Action</th><th>Outcome</th><th>Detail</th></tr></thead>
        <tbody>
          @for (row of rows(); track $index) {
            <tr class="border-t" [class.bg-destructive/10]="anomaly(row)">
              <td class="px-3 py-2 whitespace-nowrap">{{ row.at | date: 'short' }}</td>
              <td>{{ row.actorEmail }} <span class="text-muted-foreground">{{ row.actorRole }} {{ row.artUnit }}</span></td>
              <td>{{ row.action }}</td>
              <td>{{ row.outcome }} @if (anomaly(row)) { <span class="font-medium">{{ row.anomalyCode }}</span> }</td>
              <td class="max-w-md">{{ row.detail }}</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class InspectorPage {
  private readonly api = inject(Api);
  protected readonly rows = signal<AuditEvent[]>([]);
  protected action = '';
  protected actor = '';
  protected anomaliesOnly = false;
  protected readonly anomaly = isAnomaly;
  protected readonly anomalyCount = computed(() => this.rows().filter(isAnomaly).length);

  constructor() { this.load(); }

  protected load(): void {
    const params: Record<string, string> = { days: '14' };
    if (this.action) params['action'] = this.action;
    if (this.actor) params['actor'] = this.actor;
    if (this.anomaliesOnly) params['anomaly'] = 'true';
    this.api.audit(params).subscribe((rows) => this.rows.set(rows));
  }
}
