import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ZardButtonComponent } from '@/shared/components/button';
import { Api } from '../core/api';

@Component({
  selector: 'app-deadlines',
  imports: [DatePipe, RouterLink, ZardButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">Office-action deadlines</h1>
    <p class="text-muted-foreground mt-1 max-w-3xl text-sm">
      Each office action is an SQS event. EventBridge Scheduler holds a one-shot timer.
      The statutory due date is six months. This environment can fire that timer early so the Lambda path is visible.
    </p>
    <div class="mt-4 grid gap-3">
      @for (row of rows(); track row.id) {
        <article class="bg-card rounded-xl border p-4 text-sm">
          <p class="font-medium">{{ row.scheduleName }} · {{ row.status }}</p>
          <p class="text-muted-foreground">Statutory due {{ row.statutoryDueAt | date: 'mediumDate' }} · fires {{ row.scheduleFireAt | date: 'medium' }}</p>
          <div class="mt-2 flex gap-2">
            <a z-button zType="outline" zSize="sm" [routerLink]="['/app/applications', row.applicationId]">Open filing</a>
            @if (simulation()) { <button z-button zSize="sm" type="button" (click)="fire(row.id)">Fire deadline</button> }
          </div>
        </article>
      } @empty { <p class="text-muted-foreground text-sm">No open deadlines.</p> }
    </div>
    <h2 class="mt-8 font-semibold">SES notices</h2>
    <div class="mt-3 grid gap-3">
      @for (notice of notices(); track notice.sentAt + notice.subject) {
        <article class="bg-accent rounded-xl p-4 text-sm">
          <p class="font-medium">{{ notice.subject }}</p>
          <p>{{ notice.body }}</p>
          <p class="text-muted-foreground">To {{ notice.toEmail }} · {{ notice.providerMessageId }}</p>
        </article>
      }
    </div>
  `,
})
export class DeadlinesPage {
  private readonly api = inject(Api);
  protected readonly rows = signal<{ id: string; applicationId: string; scheduleName: string; status: string; statutoryDueAt: string; scheduleFireAt: string }[]>([]);
  protected readonly notices = signal<{ toEmail: string; subject: string; body: string; providerMessageId?: string; sentAt: string }[]>([]);
  protected readonly simulation = signal(false);

  constructor() {
    this.reload();
    this.api.system().subscribe((s) => this.simulation.set(s['simulationEnabled'] === true));
  }

  protected fire(id: string): void {
    this.api.simulate(id).subscribe(() => setTimeout(() => this.reload(), 900));
  }

  private reload(): void {
    this.api.deadlines().subscribe((rows) => this.rows.set(rows));
    this.api.notices().subscribe((rows) => this.notices.set(rows));
  }
}
