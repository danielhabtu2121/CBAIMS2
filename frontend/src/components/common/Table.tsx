import React from 'react';

export interface TableProps extends React.TableHTMLAttributes<HTMLTableElement> {
  striped?: boolean;
  hoverable?: boolean;
  compact?: boolean;
}

export const Table: React.FC<TableProps> = ({
  children,
  striped = false,
  hoverable = true,
  compact = false,
  className = '',
  ...props
}) => {
  return (
    <div className="w-full overflow-x-auto rounded-lg border border-slate-200/80 bg-white shadow-xs">
      <table
        className={`w-full text-left border-collapse text-slate-800 ${
          striped ? 'table-striped' : ''
        } ${hoverable ? 'table-hover' : ''} ${compact ? 'text-xs' : 'text-[13px]'} ${className}`}
        {...props}
      >
        {children}
      </table>
    </div>
  );
};

export const TableHead: React.FC<React.HTMLAttributes<HTMLTableSectionElement>> = ({
  children,
  className = '',
  ...props
}) => {
  return (
    <thead
      className={`bg-slate-50/90 border-b border-slate-200 text-slate-600 font-semibold uppercase text-[11px] tracking-wider select-none ${className}`}
      {...props}
    >
      {children}
    </thead>
  );
};

export const TableBody: React.FC<React.HTMLAttributes<HTMLTableSectionElement>> = ({
  children,
  className = '',
  ...props
}) => {
  return <tbody className={`divide-y divide-slate-100 ${className}`} {...props}>{children}</tbody>;
};

export const TableRow: React.FC<React.HTMLAttributes<HTMLTableRowElement>> = ({
  children,
  className = '',
  ...props
}) => {
  return (
    <tr className={`hover:bg-blue-50/40 transition-colors duration-150 ${className}`} {...props}>
      {children}
    </tr>
  );
};

export const TableCell: React.FC<React.TdHTMLAttributes<HTMLTableCellElement>> = ({
  children,
  className = '',
  ...props
}) => {
  return (
    <td className={`px-4 py-3 align-middle text-slate-700 ${className}`} {...props}>
      {children}
    </td>
  );
};

export const TableHeaderCell: React.FC<React.ThHTMLAttributes<HTMLTableCellElement>> = ({
  children,
  className = '',
  ...props
}) => {
  return (
    <th className={`px-4 py-2.5 font-bold text-slate-600 align-middle ${className}`} {...props}>
      {children}
    </th>
  );
};

