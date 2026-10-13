import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ZardButtonComponent } from '@/shared/components/button';
import { Api } from '../core/api';
import { bandClass, type ApplicationDetail } from '../core/models';

@Component({
  selector: 'app-detail',
  imports: [DatePipe, FormsModule, ZardButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (app(); as a) {
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p class="text-muted-foreground text-sm">{{ a.serialNumber }} · Class {{ a.internationalClass }} · {{ a.artUnit }}</p>
          <h1 class="text-3xl font-semibold">{{ a.mark }}</h1>
          <p class="mt-1 text-sm">{{ a.status }} · {{ a.ownerName }}</p>
        </div>
        <div class="rounded-2xl border px-4 py-3 text-center">
          <p class="text-3xl font-semibold">{{ a.riskScore }}</p>
          <span class="rounded-full px-2 py-0.5 text-xs" [class]="band(a.riskBand)">{{ a.riskBand }}</span>
        </div>
      </div>

      <div class="mt-6 grid gap-4 lg:grid-cols-[1.4fr_0.8fr]">
        <section class="space-y-3">
          <article class="bg-card rounded-xl border p-4">
            <h2 class="font-medium">Goods</h2>
            <p class="mt-2 text-sm leading-relaxed">{{ a.goodsDescription }}</p>
            <p class="text-muted-foreground mt-3 text-sm">{{ a.ownerAddress }} · {{ a.ownerEmail }}</p>
            @if (a.attorneyName) { <p class="text-sm">Counsel {{ a.attorneyName }} · bar {{ a.attorneyBarNumber }}</p> }
          </article>
          <article class="bg-card rounded-xl border p-4">
            <h2 class="font-medium">Rules engine</h2>
            <ul class="mt-3 space-y-2">
              @for (f of a.findings; track f.code) {
                <li class="rounded-lg border px-3 py-2 text-sm">
                  <p class="font-medium">{{ f.title }} <span class="text-muted-foreground font-normal">+{{ f.weight }}</span></p>
                  <p class="text-muted-foreground">{{ f.detail }}</p>
                </li>
              } @empty { <li class="text-muted-foreground text-sm">No rule hits.</li> }
            </ul>
          </article>
          <article class="bg-card rounded-xl border p-4">
            <h2 class="font-medium">Claude narrative <span class="text-muted-foreground font-normal">{{ a.claude?.status }}</span></h2>
            <p class="mt-2 text-sm">{{ a.claude?.summary || 'No narrative yet. The rules score is the decision of record.' }}</p>
          </article>
        </section>
        <aside class="space-y-3">
          @if (specimen(); as src) {
            <img class="bg-muted max-h-72 w-full rounded-xl border object-contain" [src]="src" alt="Specimen of use" />
          }
          @if (a.canIssueOfficeAction) {
            <form class="bg-card space-y-2 rounded-xl border p-4" (ngSubmit)="issue(a.id)">
              <h2 class="font-medium">Issue office action</h2>
              <textarea class="bg-background min-h-24 w-full rounded-md border p-2 text-sm" [(ngModel)]="actionBody" name="body" required></textarea>
              <button z-button type="submit">Publish to the deadline queue</button>
            </form>
          }
          @if (a.canRespond) {
            <form class="bg-card space-y-2 rounded-xl border p-4" (ngSubmit)="respond(a)">
              <h2 class="font-medium">File a response</h2>
              <textarea class="bg-background min-h-24 w-full rounded-md border p-2 text-sm" [(ngModel)]="responseBody" name="response" required></textarea>
              <button z-button type="submit">Submit response</button>
            </form>
          }
          @if (a.canReview) {
            <form class="bg-card space-y-2 rounded-xl border p-4" (ngSubmit)="decide(a.id, 'CONFIRM')">
              <h2 class="font-medium">OED decision</h2>
              <textarea class="bg-background min-h-20 w-full rounded-md border p-2 text-sm" [(ngModel)]="notes" name="notes"></textarea>
              <div class="flex gap-2">
                <button z-button type="submit">Confirm fraud</button>
                <button z-button zType="outline" type="button" (click)="decide(a.id, 'CLEAR')">Clear</button>
              </div>
            </form>
          }
          @if (a.canApprove) { <button z-button zType="outline" type="button" (click)="approve(a.id)">Approve</button> }
        </aside>
      </div>

      <section class="mt-6 grid gap-4 lg:grid-cols-2">
        <article class="bg-card rounded-xl border p-4">
          <h2 class="font-medium">Prosecution</h2>
          <ol class="mt-3 space-y-2 text-sm">
            @for (event of a.timeline; track event.at + event.kind) {
              <li><span class="font-medium">{{ event.kind }}</span> · {{ event.at | date: 'medium' }}<br />{{ event.detail }}</li>
            }
          </ol>
        </article>
        <article class="bg-card rounded-xl border p-4">
          <h2 class="font-medium">Office actions</h2>
          @for (oa of a.officeActions; track oa.id) {
            <div class="mt-3 rounded-lg border p-3 text-sm">
              <p class="font-medium">{{ oa.type }} · {{ oa.status }}</p>
              <p class="mt-1">{{ oa.body }}</p>
              <p class="text-muted-foreground mt-2">Statutory due {{ oa.statutoryDueAt | date: 'mediumDate' }}</p>
              <p class="text-muted-foreground">Scheduler fires {{ oa.scheduleFireAt | date: 'medium' }} · {{ oa.scheduleName }}</p>
              @if (oa.status === 'OPEN' && simulation()) {
                <button z-button zSize="sm" class="mt-2" type="button" (click)="fire(oa.id)">Fire statutory deadline</button>
              }
            </div>
          } @empty { <p class="text-muted-foreground mt-2 text-sm">No office action.</p> }
          @for (notice of a.notices; track notice.sentAt) {
            <div class="bg-accent mt-3 rounded-lg p-3 text-sm">
              <p class="font-medium">{{ notice.subject }}</p>
              <p>{{ notice.body }}</p>
              <p class="text-muted-foreground">{{ notice.providerMessageId }}</p>
            </div>
          }
        </article>
      </section>
    }
  `,
})
export class DetailPage implements OnInit {
  readonly id = input.required<string>();
  private readonly api = inject(Api);
  protected readonly app = signal<ApplicationDetail | null>(null);
  protected readonly specimen = signal<string | null>(null);
  protected readonly simulation = signal(false);
  protected readonly band = bandClass;
  protected actionBody = 'The specimen does not show the mark as used on the goods. A response is due in six months.';
  protected responseBody = '';
  protected notes = 'Pattern matches prior shell filings.';

  ngOnInit(): void {
    this.reload();
    this.api.system().subscribe((s) => this.simulation.set(s['simulationEnabled'] === true));
  }

  protected issue(id: string): void {
    this.api.issue(id, 'NONFINAL', this.actionBody).subscribe((a) => this.app.set(a));
  }

  protected respond(a: ApplicationDetail): void {
    const open = a.officeActions.find((oa) => oa.status === 'OPEN');
    if (!open) return;
    this.api.respond(open.id, this.responseBody).subscribe((next) => this.app.set(next));
  }

  protected decide(id: string, decision: string): void {
    this.api.decide(id, decision, this.notes).subscribe((a) => this.app.set(a));
  }

  protected approve(id: string): void {
    this.api.approve(id).subscribe((a) => this.app.set(a));
  }

  protected fire(officeActionId: string): void {
    this.api.simulate(officeActionId).subscribe(() => setTimeout(() => this.reload(), 800));
  }

  private reload(): void {
    const id = this.id();
    this.api.application(id).subscribe((a) => this.app.set(a));
    this.api.specimen(id).subscribe((blob) => {
      const previous = this.specimen();
      if (previous) URL.revokeObjectURL(previous);
      this.specimen.set(URL.createObjectURL(blob));
    });
  }
}
