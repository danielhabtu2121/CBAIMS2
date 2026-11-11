import React, { SelectHTMLAttributes, forwardRef, useId } from 'react';
import { ChevronDown } from 'lucide-react';

export interface SelectOption {
  value: string | number;
  label: string;
  disabled?: boolean;
}

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  hint?: string;
  error?: string;
  required?: boolean;
  options?: SelectOption[];
  placeholder?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  (
    {
      label,
      hint,
      error,
      required,
      options,
      placeholder,
      children,
      id,
      className = '',
      disabled,
      ...props
    },
    ref
  ) => {
    const generatedId = useId();
    const selectId = id || generatedId;

    return (
      <div className="w-full space-y-1.5 text-left">
        {label && (
          <label
            htmlFor={selectId}
            className="block text-xs font-semibold text-slate-700 tracking-tight"
          >
            {label}
            {required && <span className="text-red-500 ml-1 font-bold">*</span>}
          </label>
        )}

        <div className="relative flex items-center">
          <select
            ref={ref}
            id={selectId}
            disabled={disabled}
            className={`w-full rounded-md border text-sm transition-all duration-150 py-2 pl-3 pr-9 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#00205B]/20 focus:border-[#00205B] disabled:bg-slate-50 disabled:text-slate-400 disabled:cursor-not-allowed appearance-none ${error
                ? 'border-red-400 focus:border-red-500 focus:ring-red-100'
                : 'border-slate-300 hover:border-slate-400'
              } ${className}`}
            {...props}
          >
            {placeholder && (
              <option value="" disabled>
                {placeholder}
              </option>
            )}
            {options
              ? options.map((opt) => (
                <option key={opt.value} value={opt.value} disabled={opt.disabled}>
                  {opt.label}
                </option>
              ))
              : children}
          </select>

          <div className="absolute right-3 flex items-center pointer-events-none text-slate-400">
            <ChevronDown className="w-4 h-4" />
          </div>
        </div>

        {error ? (
          <p className="text-[11.5px] text-red-600 font-medium flex items-center gap-1 mt-1">
            <span>⚠️</span>
            <span>{error}</span>
          </p>
        ) : hint ? (
          <p className="text-[11.5px] text-slate-500 mt-1">{hint}</p>
        ) : null}
      </div>
    );
  }
);

Select.displayName = 'Select';

