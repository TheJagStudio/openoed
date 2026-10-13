import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ZardButtonComponent } from '@/shared/components/button';
import { Api } from '../core/api';
import { AuthStore } from '../core/auth.store';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, ZardButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-dvh">
      <header class="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
        <a routerLink="/" class="flex items-center gap-2 font-semibold tracking-tight">
          <span class="bg-primary text-primary-foreground grid size-8 place-items-center rounded-lg text-sm">OE</span>
          OpenOED
        </a>
        <nav class="flex items-center gap-2 text-sm">
          <a href="#objectives" class="text-muted-foreground hover:text-foreground px-2">Objectives</a>
          @if (auth.signedIn()) {
            <a z-button zSize="sm" routerLink="/app">Open the workbench</a>
          } @else {
            <a z-button zType="ghost" zSize="sm" routerLink="/login">Sign in</a>
            <a z-button zSize="sm" routerLink="/register">Create a free account</a>
          }
        </nav>
      </header>

      <section class="dot-grid border-y">
        <div class="mx-auto grid max-w-6xl items-center gap-10 px-6 py-16 lg:grid-cols-2">
          <div>
            <p class="text-primary text-sm font-medium">Free and open · no payments · USPTO OED / TEAS style</p>
            <h1 class="mt-3 text-4xl font-semibold leading-tight sm:text-5xl">
              Catch fraudulent trademark filings before they publish.
            </h1>
            <p class="text-muted-foreground mt-4 max-w-xl text-lg">
              A rules engine and Claude score every specimen. High-risk cases go to a priority queue.
              Office actions start a statutory six-month clock. Every mutation is written to an append-only ledger.
            </p>
            <div class="mt-6 flex flex-wrap gap-3">
              <a z-button routerLink="/register">Start filing</a>
              <a z-button zType="outline" routerLink="/login">Examiner sign in</a>
            </div>
          </div>
          <div class="bg-card rounded-2xl border p-5 shadow-sm" id="hero-card">
            <div class="flex items-center justify-between">
              <p class="text-sm font-medium">Serial 97001001 · AURORA LUMEN</p>
              <span class="band-CRITICAL rounded-full px-2 py-0.5 text-xs font-medium">CRITICAL 92</span>
            </div>
            <p class="text-muted-foreground mt-2 text-sm">Class 3 · Global Prime Holdings LLC · art unit L1</p>
            <ul class="mt-4 space-y-2 text-sm">
              <li class="bg-destructive/10 rounded-lg px-3 py-2">Shell address on the Wilmington watch list</li>
              <li class="bg-accent rounded-lg px-3 py-2">Goods description matches generative-text patterns</li>
              <li class="bg-destructive/10 rounded-lg px-3 py-2">Specimen contains a cloned image region</li>
              <li class="bg-secondary rounded-lg px-3 py-2">Attorney bar 99999 is suspended</li>
            </ul>
            <p class="text-muted-foreground mt-4 text-xs">Routed to the OED priority queue. Claude narrative is advisory.</p>
          </div>
        </div>
      </section>

      <section id="objectives" class="mx-auto grid max-w-6xl gap-4 px-6 py-16 md:grid-cols-3">
        @for (card of cards; track card.title) {
          <article class="bg-card rounded-2xl border p-5">
            <p class="text-primary text-xs font-semibold tracking-wide uppercase">{{ card.kicker }}</p>
            <h2 class="mt-2 text-xl font-semibold">{{ card.title }}</h2>
            <p class="text-muted-foreground mt-2 text-sm leading-relaxed">{{ card.body }}</p>
          </article>
        }
      </section>

      <section class="bg-primary text-primary-foreground">
        <div class="mx-auto grid max-w-6xl gap-8 px-6 py-12 md:grid-cols-3">
          <div><p class="text-3xl font-semibold">{{ stats()?.applications ?? '—' }}</p><p class="text-sm opacity-80">Applications on file</p></div>
          <div><p class="text-3xl font-semibold">{{ stats()?.priorityReviews ?? '—' }}</p><p class="text-sm opacity-80">Open priority reviews</p></div>
          <div><p class="text-3xl font-semibold">{{ stats()?.abandoned ?? '—' }}</p><p class="text-sm opacity-80">Abandoned for no response</p></div>
        </div>
      </section>

      <footer class="text-muted-foreground mx-auto max-w-6xl px-6 py-8 text-sm">
        OpenOED runs locally and maps onto Aurora PostgreSQL, SQS, EventBridge Scheduler, Lambda, SES and DynamoDB. Nothing here is a USPTO system.
      </footer>
    </div>
  `,
})
export class LandingPage {
  protected readonly auth = inject(AuthStore);
  protected readonly stats = signal<{ applications: number; priorityReviews: number; abandoned: number; notices: number } | null>(null);
  protected readonly cards = [
    { kicker: 'Objective 1', title: 'Fraud scoring', body: 'Shell addresses, reused or altered specimens, machine-written goods text and barred counsel. High scores enter the OED queue automatically.' },
    { kicker: 'Objective 2', title: 'Office-action clock', body: 'Issuing an office action publishes to SQS. EventBridge holds the six-month statutory date. No response, and Lambda marks the case abandoned and sends the notice.' },
    { kicker: 'Objective 3', title: 'Inspector ledger', body: 'An aspect records every mutation and forbidden call. The ledger is append-only. The Inspector General view flags examiners opening work outside their art unit.' },
  ];

  constructor() {
    inject(Api).stats().subscribe((s) => this.stats.set(s));
  }
}
