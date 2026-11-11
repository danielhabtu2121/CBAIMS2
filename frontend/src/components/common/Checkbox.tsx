import React, { InputHTMLAttributes, forwardRef, useId } from 'react';

export interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: React.ReactNode;
  hint?: string;
  error?: string;
}

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({ label, hint, error, id, className = '', disabled, ...props }, ref) => {
    const generatedId = useId();
    const checkboxId = id || generatedId;

    return (
      <div className="flex items-start gap-2.5 text-left">
        <div className="flex items-center h-5">
          <input
            ref={ref}
            id={checkboxId}
            type="checkbox"
            disabled={disabled}
            className={`w-4 h-4 rounded border-slate-300 text-[#00205B] focus:ring-[#00205B]/30 focus:ring-offset-0 disabled:opacity-50 disabled:cursor-not-allowed transition duration-150 cursor-pointer ${className}`}
            {...props}
          />
        </div>
        {(label || hint) && (
          <div className="text-xs">
            {label && (
              <label
                htmlFor={checkboxId}
                className={`font-medium cursor-pointer select-none ${
                  disabled ? 'text-slate-400 cursor-not-allowed' : 'text-slate-700'
                }`}
              >
                {label}
              </label>
            )}
            {hint && <p className="text-[11px] text-slate-500 mt-0.5">{hint}</p>}
            {error && <p className="text-[11px] text-red-600 mt-0.5">{error}</p>}
          </div>
        )}
      </div>
    );
  }
);

Checkbox.displayName = 'Checkbox';

export interface RadioProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: React.ReactNode;
  hint?: string;
}

export const Radio = forwardRef<HTMLInputElement, RadioProps>(
  ({ label, hint, id, className = '', disabled, ...props }, ref) => {
    const generatedId = useId();
    const radioId = id || generatedId;

    return (
      <div className="flex items-start gap-2.5 text-left">
        <div className="flex items-center h-5">
          <input
            ref={ref}
            id={radioId}
            type="radio"
            disabled={disabled}
            className={`w-4 h-4 border-slate-300 text-[#00205B] focus:ring-[#00205B]/30 focus:ring-offset-0 disabled:opacity-50 disabled:cursor-not-allowed transition duration-150 cursor-pointer ${className}`}
            {...props}
          />
        </div>
        {(label || hint) && (
          <div className="text-xs">
            {label && (
              <label
                htmlFor={radioId}
                className={`font-medium cursor-pointer select-none ${
                  disabled ? 'text-slate-400 cursor-not-allowed' : 'text-slate-700'
                }`}
              >
                {label}
              </label>
            )}
            {hint && <p className="text-[11px] text-slate-500 mt-0.5">{hint}</p>}
          </div>
        )}
      </div>
    );
  }
);

Radio.displayName = 'Radio';

