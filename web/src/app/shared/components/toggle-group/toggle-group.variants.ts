import { cva, type VariantProps } from 'class-variance-authority';

export const toggleGroupVariants = cva('inline-flex w-fit items-center gap-0.5 rounded-lg p-0.5', {
  variants: {
    zType: {
      default: 'bg-muted',
      outline: 'border bg-background',
    },
  },
  defaultVariants: { zType: 'default' },
});

export const toggleGroupItemVariants = cva(
  "inline-flex items-center justify-center gap-1.5 rounded-md font-medium whitespace-nowrap text-muted-foreground transition-all outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 aria-checked:bg-background aria-checked:text-foreground aria-checked:shadow-sm dark:aria-checked:bg-input/40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      zSize: {
        default: 'h-7 px-3 text-sm',
        sm: 'h-6 px-2 text-xs',
        lg: 'h-8 px-3.5 text-sm',
      },
    },
    defaultVariants: { zSize: 'default' },
  },
);

export type ZardToggleGroupTypeVariants = NonNullable<VariantProps<typeof toggleGroupVariants>['zType']>;
export type ZardToggleGroupSizeVariants = NonNullable<VariantProps<typeof toggleGroupItemVariants>['zSize']>;
