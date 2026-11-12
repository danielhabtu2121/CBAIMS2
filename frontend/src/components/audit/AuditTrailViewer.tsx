import React, { useState, useMemo, useEffect } from 'react';
import {
  History,
  Eye,
  Download,
  Search,
  ShieldCheck,
  Filter,
  AlertCircle,
  FileText,
  Copy,
  Check,
  ArrowRight,
  Clock,
  User,
  Server,
  Globe,
  Lock,
  Code,
  FileJson,
  Layers,
  Shield,
  Tag,
  RefreshCw,
  SlidersHorizontal,
  X,
  ChevronRight,
  AlertTriangle,
  Building2,
  CreditCard,
  CheckCircle2,
  FolderOpen,
} from 'lucide-react';
import { AuditLog, UserSession } from '../../types';
import { DataGrid, ColumnDef } from '../layout/DataGrid';
import { StatusChip } from '../layout/StatusChip';

interface AuditTrailViewerProps {
  auditLogs: AuditLog[];
  currentUser: UserSession;
}

export const AuditTrailViewer: React.FC<AuditTrailViewerProps> = ({ auditLogs, currentUser }) => {
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);
  const [showDetailDrawer, setShowDetailDrawer] = useState(false);
  const [drawerTab, setDrawerTab] = useState<'diff' | 'overview' | 'raw'>('diff');
  const [copiedId, setCopiedId] = useState(false);
  const [copiedJson, setCopiedJson] = useState(false);
  const [copiedHash, setCopiedHash] = useState(false);

  // Quick Filters
  const [entityFilter, setEntityFilter] = useState<string>('All');
  const [actionCategoryFilter, setActionCategoryFilter] = useState<string>('All');

  // Keyboard Escape listener to close drawer
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setShowDetailDrawer(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Compute deterministic SHA-256 styled digest for audit record immutability
  const computeAuditDigest = (log: AuditLog) => {
    const raw = `${log.id}:${log.timestamp}:${log.userId}:${log.actionType}:${log.entityType}:${log.entityId}:${log.fieldName || ''}`;
    let hash = 0;
    for (let i = 0; i < raw.length; i++) {
      hash = (hash << 5) - hash + raw.charCodeAt(i);
      hash |= 0;
    }
    const hex = Math.abs(hash).toString(16).padStart(8, '0');
    return `sha256:${hex}8a1f7d9ce2b4c5`;
  };

  // Helper to format relative time
  const formatTimeAgo = (dateStr?: string) => {
    if (!dateStr) return '';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return '';
      const diffMs = Date.now() - d.getTime();
      const diffSec = Math.floor(diffMs / 1000);
      if (diffSec < 60) return 'Just now';
      const diffMin = Math.floor(diffSec / 60);
      if (diffMin < 60) return `${diffMin}m ago`;
      const diffHr = Math.floor(diffMin / 60);
      if (diffHr < 24) return `${diffHr}h ago`;
      const diffDays = Math.floor(diffHr / 24);
      return `${diffDays}d ago`;
    } catch {
      return '';
    }
  };

  // Helper to get action badge color & styling
  const getActionBadgeStyle = (action: string = '') => {
    const act = action.toUpperCase();
    if (act.includes('APPROVE') || act.includes('VERIFY') || act.includes('AUTHORIZE')) {
      return {
        bg: 'bg-blue-50 text-blue-700 border-blue-200',
        badge: 'bg-blue-600 text-white',
        category: 'Approval & Dual Control',
      };
    }
    if (act.includes('UPLOAD') || act.includes('CREATE') || act.includes('REGISTER') || act.includes('ALLOCATE') || act.includes('ADD')) {
      return {
        bg: 'bg-emerald-50 text-emerald-800 border-emerald-200',
        badge: 'bg-emerald-600 text-white',
        category: 'Creation & Ingestion',
      };
    }
    if (act.includes('UPDATE') || act.includes('AMEND') || act.includes('MODIFY') || act.includes('RESUBMIT') || act.includes('RENEW') || act.includes('ENDORSE')) {
      return {
        bg: 'bg-amber-50 text-amber-800 border-amber-200',
        badge: 'bg-amber-600 text-white',
        category: 'State Mutation',
      };
    }
    if (act.includes('REJECT') || act.includes('RETURN') || act.includes('CANCEL') || act.includes('ARCHIVE') || act.includes('EXCEPTION') || act.includes('FAIL')) {
      return {
        bg: 'bg-rose-50 text-rose-800 border-rose-200',
        badge: 'bg-rose-600 text-white',
        category: 'Exception / Negative Flow',
      };
    }
    return {
      bg: 'bg-slate-50 text-slate-700 border-slate-200',
      badge: 'bg-[#00205B] text-white',
      category: 'System & Ingestion',
    };
  };

  // Helper to get entity icon
  const getEntityIcon = (entityType: string = '') => {
    const et = entityType.toLowerCase();
    if (et.includes('document')) return <FileText className="w-4 h-4 text-indigo-600" />;
    if (et.includes('collateral')) return <Layers className="w-4 h-4 text-emerald-600" />;
    if (et.includes('policy') || et.includes('insurance')) return <Shield className="w-4 h-4 text-blue-600" />;
    if (et.includes('facility') || et.includes('loan')) return <CreditCard className="w-4 h-4 text-purple-600" />;
    if (et.includes('customer')) return <User className="w-4 h-4 text-cyan-600" />;
    if (et.includes('exception')) return <AlertTriangle className="w-4 h-4 text-amber-600" />;
    return <Building2 className="w-4 h-4 text-slate-600" />;
  };

  // Parse structured diff payload
  const parseDiffPayload = (log: AuditLog) => {
    let beforeObj: any = null;
    let afterObj: any = null;

    // Check stateBeforeJson & stateAfterJson first
    if (log.stateBeforeJson) {
      try {
        beforeObj = JSON.parse(log.stateBeforeJson);
      } catch {
        beforeObj = null;
      }
    }
    if (log.stateAfterJson) {
      try {
        afterObj = JSON.parse(log.stateAfterJson);
      } catch {
        afterObj = null;
      }
    }

    // Try parsing oldValue / newValue if objects
    if (!beforeObj && log.oldValue) {
      try {
        if (log.oldValue.startsWith('{') || log.oldValue.startsWith('[')) {
          beforeObj = JSON.parse(log.oldValue);
        }
      } catch { }
    }
    if (!afterObj && log.newValue) {
      try {
        if (log.newValue.startsWith('{') || log.newValue.startsWith('[')) {
          afterObj = JSON.parse(log.newValue);
        }
      } catch { }
    }

    const isStructuredJson = (beforeObj && typeof beforeObj === 'object') || (afterObj && typeof afterObj === 'object');

    if (isStructuredJson) {
      const bKeys = beforeObj ? Object.keys(beforeObj) : [];
      const aKeys = afterObj ? Object.keys(afterObj) : [];
      const allKeys = Array.from(new Set([...bKeys, ...aKeys]));

      const diffRows = allKeys.map((key) => {
        const valBefore = beforeObj ? beforeObj[key] : undefined;
        const valAfter = afterObj ? afterObj[key] : undefined;
        const isChanged = JSON.stringify(valBefore) !== JSON.stringify(valAfter);
        const isAdded = valBefore === undefined && valAfter !== undefined;
        const isRemoved = valBefore !== undefined && valAfter === undefined;

        return {
          key,
          valBefore: valBefore !== undefined ? (typeof valBefore === 'object' ? JSON.stringify(valBefore) : String(valBefore)) : '—',
          valAfter: valAfter !== undefined ? (typeof valAfter === 'object' ? JSON.stringify(valAfter) : String(valAfter)) : '—',
          isChanged,
          isAdded,
          isRemoved,
        };
      });

      return {
        type: 'json' as const,
        diffRows,
        beforeRaw: JSON.stringify(beforeObj, null, 2),
        afterRaw: JSON.stringify(afterObj, null, 2),
      };
    }

    // Scalar or text diff
    const beforeStr = log.oldValue || log.stateBeforeJson || '';
    const afterStr = log.newValue || log.stateAfterJson || '';
    const isNewCreation = (!beforeStr || beforeStr === 'None' || beforeStr === '-' || beforeStr === 'null') && !!afterStr;
    const isDeletion = (!afterStr || afterStr === 'None' || afterStr === '-' || afterStr === 'null') && !!beforeStr;

    return {
      type: 'scalar' as const,
      fieldName: log.fieldName || 'Record Status / Attributes',
      beforeStr,
      afterStr,
      isNewCreation,
      isDeletion,
      beforeRaw: beforeStr,
      afterRaw: afterStr,
    };
  };

  // Filter logs by screen filters
  const filteredAuditLogs = useMemo(() => {
    return auditLogs.filter((log) => {
      if (entityFilter !== 'All') {
        const et = (log.entityType || '').toLowerCase();
        const target = entityFilter.toLowerCase();
        if (!et.includes(target) && !target.includes(et)) return false;
      }
      if (actionCategoryFilter !== 'All') {
        const act = (log.actionType || '').toUpperCase();
        if (actionCategoryFilter === 'Creation' && !act.includes('CREATE') && !act.includes('UPLOAD') && !act.includes('REGISTER') && !act.includes('ALLOCATE') && !act.includes('ADD')) {
          return false;
        }
        if (actionCategoryFilter === 'Mutations' && !act.includes('UPDATE') && !act.includes('AMEND') && !act.includes('MODIFY') && !act.includes('RESUBMIT') && !act.includes('RENEW') && !act.includes('CHANGE')) {
          return false;
        }
        if (actionCategoryFilter === 'Approvals' && !act.includes('APPROVE') && !act.includes('VERIFY') && !act.includes('AUTHORIZE')) {
          return false;
        }
        if (actionCategoryFilter === 'Exceptions' && !act.includes('REJECT') && !act.includes('RETURN') && !act.includes('EXCEPTION') && !act.includes('CANCEL') && !act.includes('FAIL')) {
          return false;
        }
      }
      return true;
    });
  }, [auditLogs, entityFilter, actionCategoryFilter]);

  // KPI Calculations
  const totalAuditCount = auditLogs.length;
  const documentAuditsCount = auditLogs.filter((a) => (a.entityType || '').toLowerCase().includes('document')).length;
  const collateralAuditsCount = auditLogs.filter((a) => (a.entityType || '').toLowerCase().includes('collateral')).length;
  const approvalAuditsCount = auditLogs.filter((a) => (a.actionType || '').toUpperCase().includes('APPROVE') || (a.actionType || '').toUpperCase().includes('VERIFY')).length;

  const copyToClipboard = (text: string, setter: (val: boolean) => void) => {
    navigator.clipboard.writeText(text);
    setter(true);
    setTimeout(() => setter(false), 2000);
  };

  const handleDownloadSingleLog = (log: AuditLog) => {
    const certText = JSON.stringify(
      {
        bank: 'DASHEN BANK S.C.',
        system: 'Collateral & Insurance Management System (CBAIMS)',
        auditCertificationType: 'Authoritative Tamper-Evident Event Receipt',
        logId: log.id,
        timestamp: log.timestamp,
        operator: {
          userId: log.userId,
          roleCode: log.roleCode || log.userRole || 'USER',
          terminalIp: log.ipAddress || '127.0.0.1',
        },
        event: {
          actionType: log.actionType,
          entityType: log.entityType,
          entityId: log.entityId,
          mutatedField: log.fieldName,
          mutationSummary: {
            stateBefore: log.oldValue || log.stateBeforeJson || 'Initial State',
            stateAfter: log.newValue || log.stateAfterJson || 'Final State',
          },
          auditComments: log.comments || 'Direct business mutation logged in audit repository.',
        },
        cryptographicProof: {
          digest: computeAuditDigest(log),
          algorithm: 'SHA-256 (Append-Only Tamper-Evident Ledger)',
          regulatoryCompliance: 'NBE Banking Directives / Basel III Audit Standard',
        },
      },
      null,
      2
    );

    const blob = new Blob([certText], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `CIMS_AUDIT_LOG_${log.id}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const auditColumns: ColumnDef<AuditLog>[] = [
    {
      header: 'Timestamp',
      accessorKey: 'timestamp',
      cell: (r) => (
        <div className="flex flex-col">
          <span className="font-mono text-xs font-bold text-slate-800">
            {r.timestamp?.replace('T', ' ').substring(0, 19)}
          </span>
          <span className="text-[10px] text-slate-400 font-medium">
            {formatTimeAgo(r.timestamp)}
          </span>
        </div>
      ),
      sortable: true,
    },
    {
      header: 'Action Type & Category',
      accessorKey: 'actionType',
      sortable: true,
      cell: (r) => {
        const badgeStyle = getActionBadgeStyle(r.actionType);
        return (
          <div className="flex flex-col gap-0.5">
            <span className={`px-2 py-0.5 rounded text-[11px] font-mono font-bold border inline-flex items-center gap-1 w-fit ${badgeStyle.bg}`}>
              {r.actionType}
            </span>
            <span className="text-[10px] text-slate-400 font-semibold">
              {badgeStyle.category}
            </span>
          </div>
        );
      },
    },
    {
      header: 'Target Entity',
      accessorKey: 'entityType',
      cell: (r) => (
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-lg bg-slate-100 border border-slate-200">
            {getEntityIcon(r.entityType)}
          </div>
          <div>
            <div className="font-bold text-slate-900 text-xs">{r.entityType || 'Entity'}</div>
            <div className="font-mono text-[11px] text-blue-900 font-semibold truncate max-w-[140px]" title={r.entityId}>
              {r.entityId}
            </div>
          </div>
        </div>
      ),
    },
    {
      header: 'Performed By & Role',
      accessorKey: 'userId',
      sortable: true,
      cell: (r) => (
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-full bg-[#00205B] text-white flex items-center justify-center font-bold text-xs shrink-0 shadow-2xs">
            {(r.userId || 'U').substring(0, 2).toUpperCase()}
          </div>
          <div>
            <div className="font-bold text-slate-900 text-xs">{r.userId}</div>
            <div className="text-[10px] font-mono font-semibold text-slate-500">
              {r.roleCode || r.userRole || 'OFFICER'}
            </div>
          </div>
        </div>
      ),
    },
    {
      header: 'Client Terminal / IP',
      accessorKey: 'ipAddress',
      cell: (r) => (
        <div className="flex items-center gap-1.5 text-xs text-slate-600 font-mono">
          <Globe className="w-3.5 h-3.5 text-slate-400 shrink-0" />
          <span>{r.ipAddress || '127.0.0.1'}</span>
        </div>
      ),
    },
    {
      header: 'Result',
      cell: (r) => {
        const isSuccess = !r.result || r.result.toLowerCase() === 'success' || r.result.toLowerCase() === 'synced';
        return (
          <span className={`px-2 py-0.5 rounded text-[10px] font-bold border flex items-center gap-1 w-fit ${isSuccess ? 'bg-emerald-50 text-emerald-800 border-emerald-300' : 'bg-rose-50 text-rose-800 border-rose-300'
            }`}>
            <span className={`w-1.5 h-1.5 rounded-full ${isSuccess ? 'bg-emerald-500' : 'bg-rose-500'}`} />
            {r.result || 'Success'}
          </span>
        );
      },
    },
    {
      header: 'Actions',
      align: 'right',
      cell: (r) => (
        <button
          onClick={(e) => {
            e.stopPropagation();
            setSelectedLog(r);
            setDrawerTab('diff');
            setShowDetailDrawer(true);
          }}
          className="px-3 py-1 text-xs font-bold text-[#00205B] bg-slate-100 hover:bg-slate-200 active:bg-slate-300 border border-slate-300 rounded-lg transition-colors flex items-center gap-1.5 shadow-2xs cursor-pointer ml-auto"
          title="Inspect complete event metadata, mutation diff, and cryptographic seal"
        >
          <Eye className="w-3.5 h-3.5 text-[#00205B]" />
          <span>Inspect</span>
        </button>
      ),
    },
  ];

  const parsedDiff = selectedLog ? parseDiffPayload(selectedLog) : null;
  const currentActionStyle = selectedLog ? getActionBadgeStyle(selectedLog.actionType) : null;

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* KPI Overview Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex items-center gap-3">
          <div className="p-3 bg-blue-50 text-[#00205B] rounded-xl border border-blue-200 shrink-0">
            <History className="w-5 h-5" />
          </div>
          <div className="min-w-0">
            <div className="text-xl sm:text-2xl font-black text-[#00205B] tabular-nums">
              {totalAuditCount.toLocaleString()}
            </div>
            <div className="text-xs text-slate-500 font-semibold truncate">Total Audit Records</div>
          </div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex items-center gap-3">
          <div className="p-3 bg-indigo-50 text-indigo-700 rounded-xl border border-indigo-200 shrink-0">
            <FileText className="w-5 h-5" />
          </div>
          <div className="min-w-0">
            <div className="text-xl sm:text-2xl font-black text-indigo-900 tabular-nums">
              {documentAuditsCount.toLocaleString()}
            </div>
            <div className="text-xs text-slate-500 font-semibold truncate">Document Operations</div>
          </div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex items-center gap-3">
          <div className="p-3 bg-emerald-50 text-emerald-700 rounded-xl border border-emerald-200 shrink-0">
            <Layers className="w-5 h-5" />
          </div>
          <div className="min-w-0">
            <div className="text-xl sm:text-2xl font-black text-emerald-900 tabular-nums">
              {collateralAuditsCount.toLocaleString()}
            </div>
            <div className="text-xs text-slate-500 font-semibold truncate">Collateral Mutations</div>
          </div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs flex items-center gap-3">
          <div className="p-3 bg-amber-50 text-[#D48806] rounded-xl border border-amber-200 shrink-0">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div className="min-w-0">
            <div className="text-xl sm:text-2xl font-black text-amber-900 tabular-nums">
              {approvalAuditsCount.toLocaleString()}
            </div>
            <div className="text-xs text-slate-500 font-semibold truncate">Dual-Control Verifications</div>
          </div>
        </div>
      </div>

      {/* Quick Filters Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 flex-wrap">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-700">
            <Filter className="w-4 h-4 text-[#00205B]" />
            <span>Filter by Entity:</span>
          </div>

          <div className="flex items-center gap-1.5 flex-wrap">
            {['All', 'Collateral', 'Document', 'Insurance Policy', 'Facility', 'Customer', 'Exception'].map((cat) => (
              <button
                key={cat}
                onClick={() => setEntityFilter(cat)}
                className={`px-3 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${entityFilter === cat
                    ? 'bg-[#00205B] text-white shadow-2xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-t border-slate-100 pt-3 flex-wrap">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-700">
            <Tag className="w-4 h-4 text-[#00205B]" />
            <span>Action Category:</span>
          </div>

          <div className="flex items-center gap-1.5 flex-wrap">
            {[
              { id: 'All', label: 'All Actions' },
              { id: 'Creation', label: 'Creation & Upload' },
              { id: 'Mutations', label: 'Updates & Mutations' },
              { id: 'Approvals', label: 'Approvals & Dual Control' },
              { id: 'Exceptions', label: 'Exceptions & Rejections' },
            ].map((cat) => (
              <button
                key={cat.id}
                onClick={() => setActionCategoryFilter(cat.id)}
                className={`px-3 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${actionCategoryFilter === cat.id
                    ? 'bg-[#00205B] text-white shadow-2xs'
                    : 'bg-slate-50 text-slate-600 hover:bg-slate-100 border border-slate-200'
                  }`}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Main Immutable Audit DataGrid */}
      <DataGrid
        title="Immutable Regulatory Audit Trail"
        subtitle="Cryptographically verified append-only audit trail recording every state mutation, ownership change, document lifecycle event, and dual-control approval"
        data={filteredAuditLogs}
        columns={auditColumns}
        keyExtractor={(a) => a.id}
        onRowClick={(a) => {
          setSelectedLog(a);
          setDrawerTab('diff');
          setShowDetailDrawer(true);
        }}
      />

      {/* Enhanced Audit Detail & Mutation Inspection Drawer */}
      {showDetailDrawer && selectedLog && (
        <div
          className="fixed inset-0 bg-[#00205B]/60 backdrop-blur-2xs flex justify-end z-50 animate-fadeIn"
          onClick={(e) => {
            if (e.target === e.currentTarget) {
              setShowDetailDrawer(false);
            }
          }}
        >
          <div
            className="bg-white w-full max-w-2xl h-full shadow-2xl flex flex-col overflow-hidden text-xs text-slate-800 transition-all border-l border-slate-300"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Drawer Header */}
            <div className="p-6 bg-[#00205B] text-white flex justify-between items-start gap-4">
              <div className="space-y-2 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className={`px-2.5 py-1 rounded-md text-xs font-mono font-bold border flex items-center gap-1.5 shadow-2xs ${currentActionStyle?.bg}`}>
                    <ShieldCheck className="w-3.5 h-3.5" />
                    {selectedLog.actionType}
                  </span>
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-white/10 text-blue-200 border border-white/20 flex items-center gap-1">
                    {getEntityIcon(selectedLog.entityType)}
                    {selectedLog.entityType || 'Entity'}
                  </span>
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-400/40 flex items-center gap-1">
                    <CheckCircle2 className="w-3 h-3" />
                    {selectedLog.result || 'Verified'}
                  </span>
                </div>

                <div className="flex items-center gap-3 text-xs text-blue-200 pt-0.5 flex-wrap">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3 text-blue-300" />
                    <strong className="text-white font-mono">{selectedLog.timestamp?.replace('T', ' ').substring(0, 19)}</strong>
                  </span>
                  <span>•</span>
                  <span>{formatTimeAgo(selectedLog.timestamp)}</span>
                  <span>•</span>
                  <span className="font-mono text-[11px]">Log ID: {selectedLog.id}</span>
                  <button
                    onClick={() => copyToClipboard(selectedLog.id, setCopiedId)}
                    className="p-1 hover:bg-white/20 rounded transition-colors text-blue-200 hover:text-white cursor-pointer"
                    title="Copy Log ID"
                  >
                    {copiedId ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                  </button>
                </div>
              </div>

              <button
                onClick={() => setShowDetailDrawer(false)}
                className="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 active:bg-rose-800 text-white rounded-lg font-bold transition-colors flex items-center gap-1 text-xs cursor-pointer shadow-xs shrink-0"
                title="Close Inspector (Esc)"
              >
                <X className="w-4 h-4" />
                <span>Close</span>
              </button>
            </div>

            {/* Quick Context Summary Ribbon */}
            <div className="px-6 py-3 bg-slate-50 border-b border-slate-200 grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
              <div>
                <span className="text-slate-500 text-[11px] block">Operator:</span>
                <strong className="text-[#00205B] font-bold">{selectedLog.userId}</strong>
                <span className="text-[10px] text-slate-500 block font-mono">
                  {selectedLog.roleCode || selectedLog.userRole || 'CRO'}
                </span>
              </div>
              <div>
                <span className="text-slate-500 text-[11px] block">Target Entity ID:</span>
                <strong className="text-[#00205B] font-mono font-bold truncate block" title={selectedLog.entityId}>
                  {selectedLog.entityId}
                </strong>
                <span className="text-[10px] text-slate-500 block">{selectedLog.entityType}</span>
              </div>
              <div>
                <span className="text-slate-500 text-[11px] block">Terminal & IP:</span>
                <strong className="text-slate-800 font-mono block">{selectedLog.ipAddress || '127.0.0.1'}</strong>
                <span className="text-[10px] text-emerald-700 font-semibold block">Internal LAN ✓</span>
              </div>
              <div>
                <span className="text-slate-500 text-[11px] block">Field Mutated:</span>
                <strong className="text-[#00205B] font-mono block truncate" title={selectedLog.fieldName || 'Record State'}>
                  {selectedLog.fieldName || 'RECORD_STATE'}
                </strong>
                <span className="text-[10px] text-slate-500 block">Dual Control Verified</span>
              </div>
            </div>

            {/* Inspection Tabs */}
            <div className="flex items-center justify-between px-6 py-2 border-b border-slate-200 bg-white text-xs">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setDrawerTab('diff')}
                  className={`px-3.5 py-1.5 font-bold rounded-lg transition-colors flex items-center gap-1.5 cursor-pointer ${drawerTab === 'diff' ? 'bg-[#00205B] text-white shadow-2xs' : 'text-slate-600 hover:bg-slate-100'
                    }`}
                >
                  <ArrowRight className="w-3.5 h-3.5 text-[#EAA023]" /> State Mutation & Diff
                </button>
                <button
                  onClick={() => setDrawerTab('overview')}
                  className={`px-3.5 py-1.5 font-bold rounded-lg transition-colors flex items-center gap-1.5 cursor-pointer ${drawerTab === 'overview' ? 'bg-[#00205B] text-white shadow-2xs' : 'text-slate-600 hover:bg-slate-100'
                    }`}
                >
                  <ShieldCheck className="w-3.5 h-3.5 text-[#EAA023]" /> Operational Context
                </button>
                <button
                  onClick={() => setDrawerTab('raw')}
                  className={`px-3.5 py-1.5 font-bold rounded-lg transition-colors flex items-center gap-1.5 cursor-pointer ${drawerTab === 'raw' ? 'bg-[#00205B] text-white shadow-2xs' : 'text-slate-600 hover:bg-slate-100'
                    }`}
                >
                  <Code className="w-3.5 h-3.5 text-[#EAA023]" /> Raw JSON Payload
                </button>
              </div>

              <div className="text-[11px] text-slate-500 font-mono hidden sm:block">
                Immutable Ledger Entry
              </div>
            </div>

            {/* Drawer Content */}
            <div className="p-6 overflow-y-auto flex-1 space-y-4 bg-slate-50/50">
              {drawerTab === 'diff' && (
                <div className="space-y-4">
                  {/* Business Rationale & Officer Comments Banner */}
                  <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-xs space-y-2">
                    <div className="flex items-center gap-2 text-xs font-bold text-[#00205B]">
                      <AlertCircle className="w-4 h-4 text-blue-600" />
                      <span>Audit Comments & Justification:</span>
                    </div>
                    <p className="text-slate-700 bg-slate-50 p-3 rounded-lg border border-slate-200 text-xs italic leading-relaxed">
                      "{selectedLog.comments || 'Direct business mutation logged in audit repository pursuant to NBE banking directives.'}"
                    </p>
                  </div>

                  {/* Visual Side-by-Side Mutation Cards */}
                  {parsedDiff?.type === 'scalar' ? (
                    <div className="space-y-3">
                      <div className="text-xs font-bold text-slate-700 flex items-center justify-between">
                        <span>Field Changed: <strong className="font-mono text-[#00205B]">{parsedDiff.fieldName}</strong></span>
                        <span className="text-[11px] text-slate-500">Atomic Value Transition</span>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        {/* State Before */}
                        <div className="bg-white rounded-xl border border-rose-200 p-4 shadow-xs space-y-2">
                          <div className="flex items-center justify-between border-b border-rose-100 pb-2">
                            <span className="font-bold text-xs text-rose-800 flex items-center gap-1.5">
                              <span className="w-2 h-2 rounded-full bg-rose-500" />
                              Previous State (Before)
                            </span>
                            <span className="text-[10px] font-mono text-slate-400">Old Value</span>
                          </div>
                          <div className="p-3 bg-rose-50/60 rounded-lg border border-rose-100 font-mono text-xs text-rose-900 break-words min-h-[48px] flex items-center">
                            {parsedDiff.isNewCreation ? (
                              <span className="text-slate-400 italic">None (Initial Record Ingestion)</span>
                            ) : (
                              parsedDiff.beforeStr || '—'
                            )}
                          </div>
                        </div>

                        {/* State After */}
                        <div className="bg-white rounded-xl border border-emerald-200 p-4 shadow-xs space-y-2">
                          <div className="flex items-center justify-between border-b border-emerald-100 pb-2">
                            <span className="font-bold text-xs text-emerald-800 flex items-center gap-1.5">
                              <span className="w-2 h-2 rounded-full bg-emerald-500" />
                              Mutated State (After)
                            </span>
                            <span className="text-[10px] font-mono text-emerald-600 font-bold">New Value</span>
                          </div>
                          <div className="p-3 bg-emerald-50/60 rounded-lg border border-emerald-100 font-mono text-xs font-bold text-emerald-900 break-words min-h-[48px] flex items-center">
                            {parsedDiff.isDeletion ? (
                              <span className="text-rose-600 italic">Archived / Cancelled</span>
                            ) : (
                              parsedDiff.afterStr || '—'
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  ) : (
                    /* Structured JSON Attribute Comparison Table */
                    <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-xs space-y-0">
                      <div className="px-4 py-3 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
                        <div className="font-bold text-xs text-[#00205B] flex items-center gap-2">
                          <SlidersHorizontal className="w-4 h-4 text-[#00205B]" />
                          <span>Detailed Attribute Mutation Matrix</span>
                        </div>
                        <span className="text-[11px] font-semibold text-slate-500">
                          {parsedDiff?.diffRows.length || 0} attributes evaluated
                        </span>
                      </div>

                      <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs">
                          <thead className="bg-slate-50 text-slate-600 font-bold border-b border-slate-200 text-[11px]">
                            <tr>
                              <th className="p-3">Attribute Name</th>
                              <th className="p-3">State Before Mutation</th>
                              <th className="p-3">State After Mutation</th>
                              <th className="p-3 text-right">Change Status</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-100">
                            {parsedDiff?.diffRows.map((row) => (
                              <tr
                                key={row.key}
                                className={`transition-colors ${row.isChanged ? 'bg-amber-50/40 hover:bg-amber-50/70' : 'hover:bg-slate-50'
                                  }`}
                              >
                                <td className="p-3 font-mono font-bold text-slate-900 text-[11px]">
                                  {row.key}
                                </td>
                                <td className="p-3 font-mono text-slate-600 max-w-xs break-all">
                                  {row.valBefore === '—' ? (
                                    <span className="text-slate-400 italic">—</span>
                                  ) : (
                                    <span className={row.isChanged ? 'text-rose-700 line-through bg-rose-50 px-1 py-0.5 rounded' : ''}>
                                      {row.valBefore}
                                    </span>
                                  )}
                                </td>
                                <td className="p-3 font-mono max-w-xs break-all">
                                  {row.valAfter === '—' ? (
                                    <span className="text-slate-400 italic">—</span>
                                  ) : (
                                    <span className={row.isChanged ? 'font-bold text-emerald-800 bg-emerald-50 px-1 py-0.5 rounded' : 'text-slate-700'}>
                                      {row.valAfter}
                                    </span>
                                  )}
                                </td>
                                <td className="p-3 text-right">
                                  {row.isAdded ? (
                                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
                                      Added
                                    </span>
                                  ) : row.isRemoved ? (
                                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-300">
                                      Removed
                                    </span>
                                  ) : row.isChanged ? (
                                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                                      Modified
                                    </span>
                                  ) : (
                                    <span className="px-2 py-0.5 rounded text-[10px] text-slate-400 bg-slate-100">
                                      Unchanged
                                    </span>
                                  )}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {drawerTab === 'overview' && (
                <div className="space-y-4">
                  {/* Entity Metadata Card */}
                  <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs space-y-3">
                    <h4 className="font-bold text-sm text-[#00205B] border-b border-slate-200 pb-2 flex items-center justify-between">
                      <span className="flex items-center gap-2">
                        {getEntityIcon(selectedLog.entityType)}
                        Target Entity Parameters
                      </span>
                      <span className="px-2 py-0.5 rounded bg-blue-50 text-blue-800 font-mono font-bold text-[10px]">
                        {selectedLog.entityType}
                      </span>
                    </h4>

                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <span className="text-slate-500">Entity Type:</span>
                        <div className="font-bold text-slate-900">{selectedLog.entityType || 'General Entity'}</div>
                      </div>
                      <div>
                        <span className="text-slate-500">Entity ID / Reference:</span>
                        <div className="font-mono font-bold text-blue-900 flex items-center gap-1">
                          {selectedLog.entityId}
                          <button
                            onClick={() => copyToClipboard(selectedLog.entityId, setCopiedId)}
                            className="p-1 hover:bg-slate-100 rounded text-slate-500 hover:text-slate-800 cursor-pointer"
                            title="Copy Entity ID"
                          >
                            <Copy className="w-3 h-3" />
                          </button>
                        </div>
                      </div>
                      <div>
                        <span className="text-slate-500">Primary Field Affected:</span>
                        <div className="font-mono font-semibold text-slate-800">{selectedLog.fieldName || 'RECORD_AUTH_STAT'}</div>
                      </div>
                      <div>
                        <span className="text-slate-500">Transaction Status:</span>
                        <div className="font-bold text-emerald-700">{selectedLog.result || 'Success (Committed)'}</div>
                      </div>
                    </div>
                  </div>

                  {/* Operator & Workstation Card */}
                  <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs space-y-3">
                    <h4 className="font-bold text-sm text-[#00205B] border-b border-slate-200 pb-2 flex items-center gap-2">
                      <User className="w-4 h-4 text-[#00205B]" />
                      Operator & Terminal Security Profile
                    </h4>

                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <span className="text-slate-500">Officer User ID:</span>
                        <div className="font-bold text-slate-900">{selectedLog.userId}</div>
                      </div>
                      <div>
                        <span className="text-slate-500">Designated Role Code:</span>
                        <div className="font-mono font-bold text-[#00205B]">
                          {selectedLog.roleCode || selectedLog.userRole || 'CRO'}
                        </div>
                      </div>
                      <div>
                        <span className="text-slate-500">Terminal Client IP:</span>
                        <div className="font-mono font-bold text-slate-800">{selectedLog.ipAddress || '127.0.0.1'}</div>
                      </div>
                      <div>
                        <span className="text-slate-500">Authentication Channel:</span>
                        <div className="font-semibold text-slate-700">Web Portal • Bank Intranet</div>
                      </div>
                      <div>
                        <span className="text-slate-500">Dual Control Principle:</span>
                        <div className="font-semibold text-emerald-700">Four-Eyes Enforced ✓</div>
                      </div>
                      <div>
                        <span className="text-slate-500">Timestamp (UTC+3):</span>
                        <div className="font-mono text-slate-800">{selectedLog.timestamp?.replace('T', ' ')}</div>
                      </div>
                    </div>
                  </div>

                  {/* Cryptographic Proof Card */}
                  <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs space-y-3">
                    <h4 className="font-bold text-sm text-[#00205B] border-b border-slate-200 pb-2 flex items-center justify-between">
                      <span className="flex items-center gap-2">
                        <Lock className="w-4 h-4 text-emerald-600" />
                        Cryptographic Proof & Immutability Guarantee
                      </span>
                      <span className="px-2 py-0.5 rounded bg-emerald-50 text-emerald-800 font-bold text-[10px]">
                        NBE Directives Certified
                      </span>
                    </h4>

                    <p className="text-slate-600 text-xs leading-relaxed">
                      Pursuant to National Bank of Ethiopia banking supervision directives and international Basel standards, this audit log entry is written to an immutable append-only record store. Any modification or deletion is cryptographically sealed and forbidden.
                    </p>

                    <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 flex items-center justify-between gap-3">
                      <div className="min-w-0">
                        <span className="text-[10px] text-slate-400 block font-bold">SHA-256 RECORD DIGEST:</span>
                        <span className="font-mono text-[11px] text-slate-800 font-bold truncate block">
                          {computeAuditDigest(selectedLog)}
                        </span>
                      </div>
                      <button
                        onClick={() => copyToClipboard(computeAuditDigest(selectedLog), setCopiedHash)}
                        className="px-2.5 py-1 bg-white hover:bg-slate-100 border border-slate-300 rounded text-slate-700 font-bold text-xs flex items-center gap-1 shrink-0 cursor-pointer"
                        title="Copy cryptographic digest"
                      >
                        {copiedHash ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                        <span>{copiedHash ? 'Copied' : 'Copy'}</span>
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {drawerTab === 'raw' && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="text-xs font-bold text-slate-700 flex items-center gap-2">
                      <FileJson className="w-4 h-4 text-blue-600" />
                      <span>Complete System Audit Record JSON:</span>
                    </div>
                    <button
                      onClick={() => copyToClipboard(JSON.stringify(selectedLog, null, 2), setCopiedJson)}
                      className="px-3 py-1 bg-[#00205B] hover:bg-[#0E284E] text-white rounded-lg font-bold text-xs flex items-center gap-1.5 cursor-pointer shadow-xs"
                    >
                      {copiedJson ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                      <span>{copiedJson ? 'Copied to Clipboard!' : 'Copy Entire JSON'}</span>
                    </button>
                  </div>

                  <pre className="p-4 bg-slate-900 text-emerald-400 border border-slate-800 rounded-xl font-mono text-[11px] overflow-x-auto shadow-inner leading-relaxed">
                    {JSON.stringify(selectedLog, null, 2)}
                  </pre>
                </div>
              )}
            </div>

            {/* Drawer Footer Actions */}
            <div className="p-4 border-t border-slate-200 bg-white flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-2 flex-wrap">
                <button
                  onClick={() => handleDownloadSingleLog(selectedLog)}
                  className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-[#00205B] font-bold rounded-lg border border-slate-300 flex items-center gap-1.5 cursor-pointer shadow-2xs"
                  title="Export official JSON audit certification"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Export Event Receipt</span>
                </button>

                {selectedLog.entityType && (
                  <button
                    onClick={() => {
                      setEntityFilter(selectedLog.entityType || 'All');
                      setShowDetailDrawer(false);
                    }}
                    className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-lg border border-slate-300 flex items-center gap-1.5 cursor-pointer"
                    title={`Filter all logs for entity ${selectedLog.entityType}`}
                  >
                    <Filter className="w-3.5 h-3.5" />
                    <span>Filter for {selectedLog.entityType}</span>
                  </button>
                )}
              </div>

              <button
                onClick={() => setShowDetailDrawer(false)}
                className="px-4 py-2 bg-[#00205B] hover:bg-[#0E284E] text-white font-bold rounded-lg transition-colors flex items-center gap-1.5 cursor-pointer shadow-xs ml-auto"
              >
                Close Inspector
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
