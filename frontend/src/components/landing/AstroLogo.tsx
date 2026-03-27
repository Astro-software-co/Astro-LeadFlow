import { cn } from '@/lib/utils';

interface AstroLogoProps {
  variant?: 'light' | 'dark';
  className?: string;
}

export function AstroLogo({ variant = 'dark', className }: AstroLogoProps) {
  const fillColor = variant === 'light' ? '#ffffff' : '#0f172a';

  return (
    <div className={cn('flex items-center gap-2', className)}>
      <svg
        width="32"
        height="32"
        viewBox="0 0 32 32"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        {/* Abstract 4-pointed star / rocket shape */}
        <path
          d="M16 2L19.5 12.5L30 16L19.5 19.5L16 30L12.5 19.5L2 16L12.5 12.5L16 2Z"
          fill={fillColor}
        />
        <circle cx="16" cy="16" r="4" fill={variant === 'light' ? '#0f172a' : '#ffffff'} />
      </svg>
      <span
        className={cn(
          'text-xl font-bold tracking-tight',
          variant === 'light' ? 'text-white' : 'text-slate-900'
        )}
      >
        Astro
      </span>
    </div>
  );
}
