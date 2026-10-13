import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { bandClass, type Dashboard } from '../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">Workbench</h1>
    <p class="text-muted-foreground mt-1 text-sm">Fraud queue, statutory deadlines and ledger anomalies for your role.</p>
    @if (data(); as d) {
      <section class="mt-6 grid gap-3 sm:grid-cols-4">
        <article class="bg-card rounded-xl border p-4"><p class="text-2xl font-semibold">{{ d.openReviews }}</p><p class="text-muted-foreground text-sm">Open reviews</p></article>
        <article class="bg-card rounded-xl border p-4"><p class="text-2xl font-semibold">{{ d.highRisk }}</p><p class="text-muted-foreground text-sm">High or critical</p></article>
        <article class="bg-card rounded-xl border p-4"><p class="text-2xl font-semibold">{{ d.openDeadlines }}</p><p class="text-muted-foreground text-sm">Open office actions</p></article>
        <article class="bg-card rounded-xl border p-4"><p class="text-2xl font-semibold">{{ d.anomalies }}</p><p class="text-muted-foreground text-sm">Anomalous access</p></article>
      </section>
      <section class="bg-card mt-6 overflow-hidden rounded-xl border">
        <table class="w-full text-sm">
          <thead class="bg-muted/60 text-left"><tr><th class="px-4 py-2">Serial</th><th>Mark</th><th>Status</th><th>Risk</th><th>Filed</th></tr></thead>
          <tbody>
            @for (row of d.recent; track row.id) {
              <tr class="border-t">
                <td class="px-4 py-2"><a class="text-primary underline" [routerLink]="['/app/applications', row.id]">{{ row.serialNumber }}</a></td>
                <td>{{ row.mark }}</td>
                <td>{{ row.status }}</td>
                <td><span class="rounded-full px-2 py-0.5 text-xs" [class]="band(row.riskBand)">{{ row.riskScore }} {{ row.riskBand }}</span></td>
                <td>{{ row.filedAt | date: 'mediumDate' }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  `,
})
export class DashboardPage {
  protected readonly data = signal<Dashboard | null>(null);
  protected readonly band = bandClass;
  constructor() {
    inject(Api).dashboard().subscribe((d) => this.data.set(d));
  }
}
