import React from 'react';
import { SlaStatus, WorkOrderPriority, WorkOrderStatus, WorkType } from '../../types';

const statusStyles: Record<WorkOrderStatus, string> = {
  NEW: 'bg-slate-800 text-slate-200 border-slate-600',
  ASSIGNED: 'bg-sky-950/70 text-sky-300 border-sky-800',
  IN_PROGRESS: 'bg-indigo-950/70 text-indigo-300 border-indigo-800',
  ON_HOLD: 'bg-amber-950/70 text-amber-300 border-amber-800',
  COMPLETED: 'bg-emerald-950/70 text-emerald-300 border-emerald-800',
  CLOSED: 'bg-slate-800 text-slate-300 border-slate-600',
  CANCELLED: 'bg-rose-950/70 text-rose-300 border-rose-800',
};

const priorityStyles: Record<WorkOrderPriority, string> = {
  LOW: 'bg-slate-800 text-slate-300 border-slate-600',
  MEDIUM: 'bg-sky-950/70 text-sky-300 border-sky-800',
  HIGH: 'bg-orange-950/70 text-orange-300 border-orange-800',
  URGENT: 'bg-rose-950/70 text-rose-300 border-rose-800',
};

const workTypeLabels: Record<WorkType, string> = {
  PREVENTIVE_MAINTENANCE: 'Preventive maintenance',
  CORRECTIVE_MAINTENANCE: 'Corrective maintenance',
  INSPECTION: 'Inspection',
  EMERGENCY: 'Emergency',
  INSTALLATION: 'Installation',
  OTHER: 'Other',
};

const badgeClass = (extra: string) =>
  `inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border ${extra}`;

export const WorkOrderStatusBadge: React.FC<{ status: WorkOrderStatus }> = ({ status }) => (
  <span className={badgeClass(statusStyles[status])}>{status.replace('_', ' ')}</span>
);

export const WorkOrderPriorityBadge: React.FC<{ priority: WorkOrderPriority }> = ({ priority }) => (
  <span className={badgeClass(priorityStyles[priority])}>{priority}</span>
);

const slaStyles: Record<SlaStatus, string> = {
  ON_TRACK: 'bg-emerald-950/70 text-emerald-300 border-emerald-800',
  AT_RISK: 'bg-amber-950/70 text-amber-300 border-amber-800',
  BREACHED: 'bg-rose-950/70 text-rose-300 border-rose-800',
  RESOLVED: 'bg-sky-950/70 text-sky-300 border-sky-800',
  NO_SLA: 'bg-slate-800 text-slate-300 border-slate-600',
};

export const SlaStatusBadge: React.FC<{ status?: SlaStatus | null }> = ({ status }) => (
  <span className={badgeClass(slaStyles[status || 'NO_SLA'])}>{(status || 'NO_SLA').replace('_', ' ')}</span>
);

export const formatWorkType = (workType: WorkType) => workTypeLabels[workType] || workType;

export const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

export const toDateTimeLocal = (value?: string | null) => {
  if (!value) {
    return '';
  }
  return value.length >= 16 ? value.slice(0, 16) : value;
};

export const fromDateTimeLocal = (value: string) => {
  if (!value) {
    return undefined;
  }
  return value.length === 16 ? `${value}:00` : value;
};
