import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ZardButtonComponent } from '@/shared/components/button';
import { Api } from '../core/api';
import { bandClass, type ApplicationSummary } from '../core/models';

@Component({
  selector: 'app-applications',
  imports: [RouterLink, FormsModule, ZardButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-2xl font-semibold">Applications</h1>
        <p class="text-muted-foreground text-sm">Applicants see their own filings. Staff see the whole docket.</p>
      </div>
      <a z-button routerLink="/app/file">New filing</a>
    </div>
    <form class="mt-4 flex flex-wrap gap-2" (ngSubmit)="load()">
      <input class="bg-card h-9 rounded-md border px-3 text-sm" name="q" placeholder="Mark, serial or owner" [(ngModel)]="q" />
      <select class="bg-card h-9 rounded-md border px-2 text-sm" name="band" [(ngModel)]="band">
        <option value="">Any risk</option>
        <option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option>
      </select>
      <button z-button zType="outline" type="submit">Filter</button>
    </form>
    <div class="mt-4 grid gap-3">
      @for (row of rows(); track row.id) {
        <a class="bg-card hover:border-primary/40 block rounded-xl border p-4" [routerLink]="['/app/applications', row.id]">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <p class="font-medium">{{ row.mark }} <span class="text-muted-foreground font-normal">{{ row.serialNumber }}</span></p>
            <span class="rounded-full px-2 py-0.5 text-xs" [class]="bandClass(row.riskBand)">{{ row.riskScore }} {{ row.riskBand }}</span>
          </div>
          <p class="text-muted-foreground mt-1 text-sm">Class {{ row.internationalClass }} · {{ row.artUnit }} · {{ row.status }} · {{ row.ownerName }}</p>
        </a>
      } @empty {
        <p class="text-muted-foreground text-sm">No applications match.</p>
      }
    </div>
  `,
})
export class ApplicationsPage {
  private readonly api = inject(Api);
  protected readonly rows = signal<ApplicationSummary[]>([]);
  protected readonly bandClass = bandClass;
  protected q = '';
  protected band = '';

  constructor() {
    this.load();
  }

  protected load(): void {
    const params: Record<string, string> = {};
    if (this.q) params['q'] = this.q;
    if (this.band) params['band'] = this.band;
    this.api.applications(params).subscribe((rows) => this.rows.set(rows));
  }
}
