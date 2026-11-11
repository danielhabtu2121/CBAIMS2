import React, { ButtonHTMLAttributes, forwardRef } from 'react';
import { Loader2 } from 'lucide-react';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'accent';
export type ButtonSize = 'xs' | 'sm' | 'md' | 'lg';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  icon?: React.ReactNode;
  iconPosition?: 'left' | 'right';
  fullWidth?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      children,
      variant = 'primary',
      size = 'md',
      loading = false,
      icon,
      iconPosition = 'left',
      fullWidth = false,
      className = '',
      disabled,
      ...props
    },
    ref
  ) => {
    // Size Classes
    const sizeClasses: Record<ButtonSize, string> = {
      xs: 'text-[11px] px-2 py-1 gap-1.5 rounded-sm',
      sm: 'text-xs px-3 py-1.5 gap-1.5 rounded-md font-medium',
      md: 'text-[13px] px-4 py-2 gap-2 rounded-md font-semibold',
      lg: 'text-sm px-5 py-2.5 gap-2.5 rounded-lg font-semibold',
    };

    // Variant Classes
    const variantClasses: Record<ButtonVariant, string> = {
      primary:
        'bg-[#00205B] text-white hover:bg-[#0E284E] active:bg-[#07152B] border border-[#00205B] shadow-xs hover:shadow-sm focus-visible:ring-2 focus-visible:ring-[#00205B] focus-visible:ring-offset-2',
      secondary:
        'bg-white text-[#00205B] hover:bg-slate-50 active:bg-slate-100 border border-slate-300 shadow-xs focus-visible:ring-2 focus-visible:ring-slate-300 focus-visible:ring-offset-2',
      outline:
        'bg-transparent text-[#00205B] hover:bg-blue-50/70 border border-[#1F4E7A] focus-visible:ring-2 focus-visible:ring-[#1F4E7A] focus-visible:ring-offset-2',
      ghost:
        'bg-transparent text-[#00205B] hover:bg-slate-100/80 active:bg-slate-200/60 border border-transparent focus-visible:ring-2 focus-visible:ring-slate-300 focus-visible:ring-offset-2',
      danger:
        'bg-red-600 text-white hover:bg-red-700 active:bg-red-800 border border-red-600 shadow-xs focus-visible:ring-2 focus-visible:ring-red-600 focus-visible:ring-offset-2',
      accent:
        'bg-[#D48806] text-white hover:bg-[#B87802] active:bg-[#9E6700] border border-[#D48806] shadow-xs hover:shadow-sm focus-visible:ring-2 focus-visible:ring-[#D48806] focus-visible:ring-offset-2',
    };

    const baseClasses =
      'inline-flex items-center justify-center transition-all duration-150 select-none outline-none disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none active:translate-y-px';

    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={`${baseClasses} ${sizeClasses[size]} ${variantClasses[variant]} ${
          fullWidth ? 'w-full' : ''
        } ${className}`}
        {...props}
      >
        {loading && <Loader2 className="w-4 h-4 animate-spin shrink-0" />}
        {!loading && icon && iconPosition === 'left' && <span className="shrink-0">{icon}</span>}
        {children && <span>{children}</span>}
        {!loading && icon && iconPosition === 'right' && <span className="shrink-0">{icon}</span>}
      </button>
    );
  }
);

Button.displayName = 'Button';

