import { cva, type VariantProps } from 'class-variance-authority';

/**
 * ZardUI-style slider built on a native `<input type="range">` (keyboard, touch and screen-reader
 * support for free). The filled track is painted with a CSS gradient driven by `--z-slider-fill`.
 */
export const sliderVariants = cva(
  [
    'relative w-full cursor-pointer appearance-none bg-transparent outline-none disabled:cursor-not-allowed disabled:opacity-50',
    '[&::-webkit-slider-runnable-track]:rounded-full [&::-webkit-slider-runnable-track]:bg-[linear-gradient(to_right,var(--primary)_var(--z-slider-fill),var(--muted)_var(--z-slider-fill))]',
    '[&::-moz-range-track]:rounded-full [&::-moz-range-track]:bg-muted [&::-moz-range-progress]:rounded-full [&::-moz-range-progress]:bg-primary',
    '[&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:border [&::-webkit-slider-thumb]:border-primary [&::-webkit-slider-thumb]:bg-background [&::-webkit-slider-thumb]:shadow-sm [&::-webkit-slider-thumb]:transition-[box-shadow]',
    '[&::-moz-range-thumb]:rounded-full [&::-moz-range-thumb]:border [&::-moz-range-thumb]:border-primary [&::-moz-range-thumb]:bg-background [&::-moz-range-thumb]:shadow-sm',
    'focus-visible:[&::-webkit-slider-thumb]:ring-[3px] focus-visible:[&::-webkit-slider-thumb]:ring-ring/50 hover:[&::-webkit-slider-thumb]:ring-4 hover:[&::-webkit-slider-thumb]:ring-ring/30',
    'focus-visible:[&::-moz-range-thumb]:ring-[3px] focus-visible:[&::-moz-range-thumb]:ring-ring/50',
  ].join(' '),
  {
    variants: {
      zSize: {
        default:
          'h-4 [&::-webkit-slider-runnable-track]:h-1.5 [&::-moz-range-track]:h-1.5 [&::-moz-range-progress]:h-1.5 [&::-webkit-slider-thumb]:-mt-[5px] [&::-webkit-slider-thumb]:size-4 [&::-moz-range-thumb]:size-4',
        sm: 'h-3 [&::-webkit-slider-runnable-track]:h-1 [&::-moz-range-track]:h-1 [&::-moz-range-progress]:h-1 [&::-webkit-slider-thumb]:-mt-[4px] [&::-webkit-slider-thumb]:size-3 [&::-moz-range-thumb]:size-3',
      },
    },
    defaultVariants: { zSize: 'default' },
  },
);

export type ZardSliderSizeVariants = NonNullable<VariantProps<typeof sliderVariants>['zSize']>;
