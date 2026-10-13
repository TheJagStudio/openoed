import { JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Api } from '../core/api';
import { AuthStore } from '../core/auth.store';

@Component({
  selector: 'app-system',
  imports: [JsonPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">System</h1>
    <p class="text-muted-foreground mt-1 text-sm">Local stand-ins for SQS, EventBridge Scheduler, Lambda, SES and the DynamoDB ledger.</p>
    @if (info(); as i) {
      <dl class="mt-6 grid gap-3 sm:grid-cols-2">
        <div class="bg-card rounded-xl border p-4"><dt class="text-muted-foreground text-sm">Claude</dt><dd class="text-lg font-medium">{{ i['claudeEnabled'] ? 'Configured' : 'Rules only' }}</dd></div>
        <div class="bg-card rounded-xl border p-4"><dt class="text-muted-foreground text-sm">Statutory period</dt><dd class="text-lg font-medium">{{ i['statutoryMonths'] }} months</dd></div>
        <div class="bg-card rounded-xl border p-4"><dt class="text-muted-foreground text-sm">Scheduler delay</dt><dd class="text-lg font-medium">{{ i['schedulerDelay'] }}</dd></div>
        <div class="bg-card rounded-xl border p-4"><dt class="text-muted-foreground text-sm">Platform</dt><dd class="text-lg font-medium">{{ i['platformUp'] ? 'Up' : 'Down' }}</dd></div>
      </dl>
      <pre class="bg-card mt-4 overflow-auto rounded-xl border p-4 text-xs">{{ i['platform'] | json }}</pre>
    }
    <h2 class="mt-8 font-semibold">Shell-address watch list</h2>
    <ul class="mt-3 space-y-2 text-sm">
      @for (row of shells(); track row.id) {
        <li class="bg-card rounded-lg border px-3 py-2">{{ row.address }} <span class="text-muted-foreground">{{ row.note }}</span></li>
      }
    </ul>
  `,
})
export class SystemPage {
  private readonly api = inject(Api);
  protected readonly info = signal<Record<string, unknown> | null>(null);
  protected readonly shells = signal<{ id: string; address: string; note?: string }[]>([]);
  constructor() {
    this.api.system().subscribe((info) => this.info.set(info));
    const role = inject(AuthStore).user()?.role;
    if (role === 'ADMIN' || role === 'OED') this.api.shells().subscribe((rows) => this.shells.set(rows));
  }
}
