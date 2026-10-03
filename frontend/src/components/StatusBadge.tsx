import React from 'react';

interface StatusBadgeProps {
  status: 'ACTIVE' | 'INACTIVE';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const active = status === 'ACTIVE';

  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border ${
        active
          ? 'bg-emerald-950/70 text-emerald-300 border-emerald-800'
          : 'bg-slate-800 text-slate-300 border-slate-600'
      }`}
    >
      {status}
    </span>
  );
};
