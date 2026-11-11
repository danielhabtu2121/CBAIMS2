import React from 'react';
import { ChevronRight, Home } from 'lucide-react';
import { NavPageId } from './Sidebar';

interface BreadcrumbsProps {
  activePage: NavPageId;
  detailItemName?: string;
  onNavigateHome: () => void;
}

export const Breadcrumbs: React.FC<BreadcrumbsProps> = ({ activePage, detailItemName, onNavigateHome }) => {
  const pageTitles: Record<NavPageId, string> = {
    dashboard: 'Dashboard',
    customers: 'Customer Master',
    facilities: 'Credit Facilities & Loans',
    collateral: 'Collateral Management',
    insurance: 'Insurance Policies',
    approvals: 'Workflow Approvals',
    exceptions: 'Exception Management',
    documents: 'Document Repository (DMS)',
    reports: 'Reports & Analytics',
    notifications: 'Notification Management',
    admin: 'System Administration',
    audit: 'Immutable Audit Trail',
    'cbs-sim': 'Core Banking System (CBS) Simulator',
  };

  return (
    <nav
      aria-label="Breadcrumb"
      className="bg-white/80 backdrop-blur-xs px-6 py-2.5 border-b border-slate-200/80 flex items-center gap-1.5 text-xs text-slate-500 select-none"
    >
      <button
        onClick={onNavigateHome}
        className="inline-flex items-center gap-1.5 text-slate-600 hover:text-[#00205B] font-semibold transition-colors focus:outline-none focus:ring-1 focus:ring-[#00205B] rounded px-1 -mx-1"
      >
        <Home className="w-3.5 h-3.5 text-slate-400 group-hover:text-[#00205B]" />
        <span>Home</span>
      </button>

      <ChevronRight className="w-3.5 h-3.5 text-slate-400 shrink-0" />

      <span className={`transition-colors ${detailItemName ? 'font-medium text-slate-600' : 'font-bold text-slate-900'}`}>
        {pageTitles[activePage] || activePage}
      </span>

      {detailItemName && (
        <>
          <ChevronRight className="w-3.5 h-3.5 text-slate-400 shrink-0" />
          <span className="font-bold text-slate-900 truncate max-w-xs">{detailItemName}</span>
        </>
      )}
    </nav>
  );
};
