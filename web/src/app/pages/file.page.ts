import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ZardButtonComponent } from '@/shared/components/button';
import { Api } from '../core/api';

@Component({
  selector: 'app-file',
  imports: [FormsModule, ZardButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">File a trademark</h1>
    <p class="text-muted-foreground mt-1 max-w-2xl text-sm">The rules engine scores the filing before you leave this page. Claude adds a narrative when an API key is configured.</p>
    <form class="mt-6 grid max-w-3xl gap-3" (ngSubmit)="submit()">
      <div class="grid gap-3 sm:grid-cols-2">
        <label class="field">Mark <input name="mark" [(ngModel)]="mark" required /></label>
        <label class="field">Class <input name="cls" type="number" min="1" max="45" [(ngModel)]="internationalClass" required /></label>
      </div>
      <label class="field">Goods <textarea name="goods" [(ngModel)]="goods" required></textarea></label>
      <div class="grid gap-3 sm:grid-cols-2">
        <label class="field">Owner <input name="owner" [(ngModel)]="ownerName" required /></label>
        <label class="field">Owner email <input name="email" type="email" [(ngModel)]="ownerEmail" required /></label>
      </div>
      <label class="field">Address <input name="address" [(ngModel)]="ownerAddress" required /></label>
      <div class="grid gap-3 sm:grid-cols-2">
        <label class="field">Attorney <input name="attorney" [(ngModel)]="attorneyName" /></label>
        <label class="field">Bar number <input name="bar" [(ngModel)]="attorneyBar" /></label>
      </div>
      <label class="field">Specimen of use <input name="specimen" type="file" accept="image/png,image/jpeg" (change)="onFile($event)" required /></label>
      <div class="flex flex-wrap gap-2">
        <button z-button type="button" zType="outline" (click)="sample()">Load a suspicious sample</button>
        <button z-button type="submit" [disabled]="busy()">Submit for scoring</button>
      </div>
    </form>
  `,
})
export class FilePage {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  protected mark = '';
  protected internationalClass = 3;
  protected goods = '';
  protected ownerName = '';
  protected ownerEmail = '';
  protected ownerAddress = '';
  protected attorneyName = '';
  protected attorneyBar = '';
  protected readonly busy = signal(false);
  private file: File | null = null;

  protected sample(): void {
    this.mark = 'AURORA LUMEN';
    this.internationalClass = 3;
    this.goods = 'Elevate your everyday with our cutting-edge, seamless, unparalleled skincare elixir designed to transform your routine.';
    this.ownerName = 'Global Prime Holdings LLC';
    this.ownerEmail = 'filings@example.com';
    this.ownerAddress = '1234 Market Street, Suite 400, Wilmington, DE 19801';
    this.attorneyName = 'Morgan Shell';
    this.attorneyBar = '99999';
  }

  protected onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files?.[0] ?? null;
  }

  protected submit(): void {
    if (!this.file) return;
    this.busy.set(true);
    this.api
      .file(
        {
          mark: this.mark,
          markType: 'STANDARD',
          internationalClass: Number(this.internationalClass),
          goodsDescription: this.goods,
          ownerName: this.ownerName,
          ownerAddress: this.ownerAddress,
          ownerEmail: this.ownerEmail,
          attorneyName: this.attorneyName,
          attorneyBarNumber: this.attorneyBar,
        },
        this.file,
      )
      .subscribe({
        next: (created) => void this.router.navigate(['/app/applications', created.id]),
        error: () => this.busy.set(false),
      });
  }
}
