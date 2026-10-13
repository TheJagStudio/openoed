import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  forwardRef,
  inject,
  input,
  model,
  signal,
  ViewEncapsulation,
} from '@angular/core';
import { type ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { NgIcon } from '@ng-icons/core';
import type { ClassValue } from 'clsx';

import { mergeClasses } from '@/shared/utils/merge-classes';
import { noopFn } from '@/shared/utils/noop';

import {
  toggleGroupItemVariants,
  toggleGroupVariants,
  type ZardToggleGroupSizeVariants,
  type ZardToggleGroupTypeVariants,
} from './toggle-group.variants';

export interface ZardToggleOption<T extends string = string> {
  value: T;
  label: string;
  icon?: string;
  disabled?: boolean;
  /** Tooltip / accessible description. */
  description?: string;
}

/**
 * Single-select segmented control (`role="radiogroup"`, roving tabindex, arrow keys).
 * `<z-toggle-group [zOptions]="opts" [(zValue)]="mode" zAriaLabel="Search mode" />`
 * Also a ControlValueAccessor. Icons must be provided by the host (`provideIcons`).
 */
@Component({
  selector: 'z-toggle-group',
  imports: [NgIcon],
  template: `
    <div role="radiogroup" [attr.aria-label]="zAriaLabel() || null" [class]="groupClasses()" (keydown)="onKeydown($event)">
      @for (opt of zOptions(); track opt.value; let i = $index) {
        <button
          type="button"
          role="radio"
          [class]="itemClasses()"
          [attr.aria-checked]="zValue() === opt.value"
          [attr.tabindex]="isFocusTarget(opt.value, i) ? 0 : -1"
          [attr.title]="opt.description || null"
          [attr.data-value]="opt.value"
          [disabled]="zDisabled() || formDisabled() || !!opt.disabled"
          (click)="select(opt.value)"
        >
          @if (opt.icon) {
            <ng-icon [name]="opt.icon" aria-hidden="true" />
          }
          {{ opt.label }}
        </button>
      }
    </div>
  `,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => ZardToggleGroupComponent), multi: true }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  host: { 'data-slot': 'toggle-group', class: 'inline-block' },
  exportAs: 'zToggleGroup',
})
export class ZardToggleGroupComponent implements ControlValueAccessor {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly class = input<ClassValue>('');
  readonly zOptions = input.required<ZardToggleOption[]>();
  readonly zValue = model<string | null>(null);
  readonly zType = input<ZardToggleGroupTypeVariants>('default');
  readonly zSize = input<ZardToggleGroupSizeVariants>('default');
  readonly zDisabled = input(false, { transform: booleanAttribute });
  readonly zAriaLabel = input<string>('');

  protected readonly formDisabled = signal(false);
  private onChange: (v: string) => void = noopFn;
  private onTouched: () => void = noopFn;

  protected readonly groupClasses = computed(() => mergeClasses(toggleGroupVariants({ zType: this.zType() }), this.class()));
  protected readonly itemClasses = computed(() => toggleGroupItemVariants({ zSize: this.zSize() }));

  protected isFocusTarget(value: string, index: number): boolean {
    const current = this.zValue();
    return current === null || !this.zOptions().some((o) => o.value === current) ? index === 0 : current === value;
  }

  protected select(value: string): void {
    if (this.zValue() === value) return;
    this.zValue.set(value);
    this.onChange(value);
    this.onTouched();
  }

  protected onKeydown(event: KeyboardEvent): void {
    const keys = ['ArrowRight', 'ArrowDown', 'ArrowLeft', 'ArrowUp', 'Home', 'End'];
    if (!keys.includes(event.key)) return;
    const enabled = this.zOptions().filter((o) => !o.disabled);
    if (!enabled.length) return;
    event.preventDefault();
    const idx = Math.max(0, enabled.findIndex((o) => o.value === this.zValue()));
    const next =
      event.key === 'Home'
        ? 0
        : event.key === 'End'
          ? enabled.length - 1
          : event.key === 'ArrowRight' || event.key === 'ArrowDown'
            ? (idx + 1) % enabled.length
            : (idx - 1 + enabled.length) % enabled.length;
    const value = enabled[next]!.value;
    this.select(value);
    queueMicrotask(() => this.host.nativeElement.querySelector<HTMLButtonElement>(`button[data-value="${CSS.escape(value)}"]`)?.focus());
  }

  writeValue(value: string | null): void {
    this.zValue.set(value);
  }

  registerOnChange(fn: (v: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.formDisabled.set(isDisabled);
  }
}
