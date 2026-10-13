import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  computed,
  forwardRef,
  input,
  model,
  numberAttribute,
  signal,
  ViewEncapsulation,
} from '@angular/core';
import { type ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import type { ClassValue } from 'clsx';

import { mergeClasses } from '@/shared/utils/merge-classes';
import { noopFn } from '@/shared/utils/noop';

import { sliderVariants, type ZardSliderSizeVariants } from './slider.variants';

/**
 * `<z-slider [(zValue)]="topK" [zMin]="1" [zMax]="50" zAriaLabel="Top K" />`
 * Also a ControlValueAccessor (`formControlName`). Native range input underneath.
 */
@Component({
  selector: 'z-slider',
  template: `
    <input
      type="range"
      [id]="zId() || null"
      [class]="classes()"
      [min]="zMin()"
      [max]="zMax()"
      [step]="zStep()"
      [value]="zValue()"
      [disabled]="zDisabled() || formDisabled()"
      [attr.aria-label]="zAriaLabel() || null"
      [attr.aria-labelledby]="zAriaLabelledBy() || null"
      [attr.aria-valuetext]="zValueText() || null"
      [style.--z-slider-fill]="fill()"
      (input)="onInput($event)"
      (blur)="onTouched()"
    />
  `,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => ZardSliderComponent), multi: true }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  host: { 'data-slot': 'slider', class: 'flex w-full items-center' },
  exportAs: 'zSlider',
})
export class ZardSliderComponent implements ControlValueAccessor {
  readonly class = input<ClassValue>('');
  readonly zValue = model<number>(0);
  readonly zMin = input(0, { transform: numberAttribute });
  readonly zMax = input(100, { transform: numberAttribute });
  readonly zStep = input(1, { transform: numberAttribute });
  readonly zId = input<string>('');
  readonly zSize = input<ZardSliderSizeVariants>('default');
  readonly zDisabled = input(false, { transform: booleanAttribute });
  readonly zAriaLabel = input<string>('');
  readonly zAriaLabelledBy = input<string>('');
  /** Human description of the value for screen readers (e.g. "10 results"). */
  readonly zValueText = input<string>('');

  protected readonly formDisabled = signal(false);
  private onChange: (v: number) => void = noopFn;
  protected onTouched: () => void = noopFn;

  protected readonly classes = computed(() => mergeClasses(sliderVariants({ zSize: this.zSize() }), this.class()));
  protected readonly fill = computed(() => {
    const span = this.zMax() - this.zMin();
    const pct = span > 0 ? ((this.zValue() - this.zMin()) / span) * 100 : 0;
    return `${Math.max(0, Math.min(100, pct))}%`;
  });

  protected onInput(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.zValue.set(value);
    this.onChange(value);
  }

  writeValue(value: number | null): void {
    this.zValue.set(Number(value ?? this.zMin()));
  }

  registerOnChange(fn: (v: number) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.formDisabled.set(isDisabled);
  }
}
