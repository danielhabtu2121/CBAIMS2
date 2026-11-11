import React from 'react';

export type BadgeVariant =
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'neutral'
  | 'brand'
  | 'accent';

export type BadgeSize = 'sm' | 'md';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  size?: BadgeSize;
  dot?: boolean;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'neutral',
  size = 'md',
  dot = false,
  className = '',
  ...props
}) => {
  const sizeClasses: Record<BadgeSize, string> = {
    sm: 'text-[11px] px-2 py-0.5 gap-1.5',
    md: 'text-xs px-2.5 py-1 gap-1.5',
  };

  const variantClasses: Record<BadgeVariant, { container: string; dot: string }> = {
    success: {
      container: 'bg-emerald-50 text-emerald-800 border-emerald-200/80',
      dot: 'bg-emerald-500',
    },
    warning: {
      container: 'bg-amber-50 text-amber-800 border-amber-200/80',
      dot: 'bg-amber-500',
    },
    danger: {
      container: 'bg-red-50 text-red-800 border-red-200/80',
      dot: 'bg-red-500',
    },
    info: {
      container: 'bg-sky-50 text-sky-800 border-sky-200/80',
      dot: 'bg-sky-500',
    },
    neutral: {
      container: 'bg-slate-100 text-slate-700 border-slate-200',
      dot: 'bg-slate-400',
    },
    brand: {
      container: 'bg-blue-50 text-[#00205B] border-blue-200',
      dot: 'bg-[#00205B]',
    },
    accent: {
      container: 'bg-amber-50 text-[#9E6700] border-amber-300',
      dot: 'bg-[#D48806]',
    },
  };

  return (
    <span
      className={`inline-flex items-center font-semibold rounded-full border whitespace-nowrap transition-colors duration-150 ${sizeClasses[size]} ${variantClasses[variant].container} ${className}`}
      {...props}
    >
      {dot && <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${variantClasses[variant].dot}`} />}
      <span>{children}</span>
    </span>
  );
};

