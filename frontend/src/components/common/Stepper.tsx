import React from 'react';
import { Check } from 'lucide-react';

export interface StepItem {
  id: number | string;
  title: string;
  subtitle?: string;
}

export interface StepperProps {
  steps: StepItem[];
  currentStep: number;
  onStepClick?: (stepIndex: number) => void;
  clickable?: boolean;
  className?: string;
}

export const Stepper: React.FC<StepperProps> = ({
  steps,
  currentStep,
  onStepClick,
  clickable = false,
  className = '',
}) => {
  return (
    <div className={`w-full overflow-x-auto py-2 custom-scrollbar select-none ${className}`}>
      <div className="flex items-center min-w-max justify-between gap-2 px-2">
        {steps.map((step, idx) => {
          const stepNumber = idx + 1;
          const isCompleted = stepNumber < currentStep;
          const isCurrent = stepNumber === currentStep;
          const isPending = stepNumber > currentStep;

          return (
            <React.Fragment key={step.id}>
              {/* Step indicator node */}
              <div
                onClick={() => {
                  if (clickable && onStepClick && (isCompleted || isCurrent)) {
                    onStepClick(stepNumber);
                  }
                }}
                className={`flex items-center gap-2.5 transition-all ${clickable && (isCompleted || isCurrent) ? 'cursor-pointer' : 'cursor-default'
                  }`}
              >
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all shadow-xs shrink-0 ${isCompleted
                      ? 'bg-emerald-600 text-white'
                      : isCurrent
                        ? 'bg-[#00205B] text-white ring-4 ring-blue-100'
                        : 'bg-slate-100 text-slate-500 border border-slate-300'
                    }`}
                >
                  {isCompleted ? <Check className="w-4 h-4 stroke-[2.5]" /> : stepNumber}
                </div>

                <div className="text-left">
                  <div
                    className={`text-xs font-bold leading-tight ${isCurrent
                        ? 'text-[#00205B]'
                        : isCompleted
                          ? 'text-slate-800'
                          : 'text-slate-400'
                      }`}
                  >
                    {step.title}
                  </div>
                  {step.subtitle && (
                    <div className="text-[10px] text-slate-400 font-medium">
                      {step.subtitle}
                    </div>
                  )}
                </div>
              </div>

              {/* Connecting line */}
              {idx < steps.length - 1 && (
                <div
                  className={`h-0.5 flex-1 min-w-[20px] max-w-[60px] mx-1 transition-colors ${isCompleted ? 'bg-emerald-500' : 'bg-slate-200'
                    }`}
                />
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
};

