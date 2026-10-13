import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ZardSonnerComponent } from '@/shared/components/sonner';
import { ZardDarkMode } from '@/shared/services/dark-mode';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ZardSonnerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<router-outlet /><z-sonner position="bottom-right" [closeButton]="true" [richColors]="true" />`,
})
export class App {
  constructor() {
    inject(ZardDarkMode).init();
  }
}
