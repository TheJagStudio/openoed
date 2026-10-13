import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { bandClass, type ApplicationSummary } from '../core/models';

@Component({
  selector: 'app-queue',
  imports: [RouterLink, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">OED priority queue</h1>
    <p class="text-muted-foreground mt-1 text-sm">Filings at 50 or above are routed here automatically.</p>
    <label class="mt-4 inline-flex items-center gap-2 text-sm">Status
      <select class="bg-card h-9 rounded-md border px-2" [(ngModel)]="status" (ngModelChange)="load()">
        <option>OPEN</option><option>ASSIGNED</option><option>CONFIRMED</option><option>CLEARED</option>
      </select>
    </label>
    <div class="mt-4 grid gap-3">
      @for (row of rows(); track row.reviewId) {
        <a class="bg-card block rounded-xl border p-4" [routerLink]="['/app/applications', row.application.id]">
          <div class="flex items-center justify-between">
            <p class="font-medium">{{ row.application.mark }}</p>
            <span class="rounded-full px-2 py-0.5 text-xs" [class]="band(row.application.riskBand)">{{ row.priority }} {{ row.application.riskBand }}</span>
          </div>
          <p class="text-muted-foreground text-sm">{{ row.application.serialNumber }} · {{ row.reviewStatus }} · {{ row.application.ownerName }}</p>
        </a>
      } @empty { <p class="text-muted-foreground text-sm">The queue is clear.</p> }
    </div>
  `,
})
export class QueuePage {
  private readonly api = inject(Api);
  protected status = 'OPEN';
  protected readonly rows = signal<{ reviewId: string; reviewStatus: string; priority: number; application: ApplicationSummary }[]>([]);
  protected readonly band = bandClass;
  constructor() { this.load(); }
  protected load(): void { this.api.reviews(this.status).subscribe((rows) => this.rows.set(rows)); }
}
