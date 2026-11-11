import React from 'react';
import { Inbox } from 'lucide-react';

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
  compact?: boolean;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  action,
  compact = false,
}) => {
  return (
    <div
      className={`flex flex-col items-center justify-center text-center rounded-xl border border-dashed border-slate-300 bg-slate-50/50 ${compact ? 'p-6' : 'p-10'
        }`}
    >
      <div className="w-12 h-12 rounded-full bg-blue-50 text-[#00205B] flex items-center justify-center mb-3">
        {icon || <Inbox className="w-6 h-6 text-[#1F4E7A]" />}
      </div>
      <h4 className="text-sm font-bold text-slate-800 tracking-tight">{title}</h4>
      {description && <p className="text-xs text-slate-500 max-w-sm mt-1">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
};

