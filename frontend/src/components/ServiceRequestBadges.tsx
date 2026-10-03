import React from 'react';
import { ServiceRequestStatus, WorkOrderPriority } from '../types';

const statusStyles: Record<ServiceRequestStatus, string> = {
  SUBMITTED: 'bg-sky-950/70 text-sky-300 border-sky-800',
  ACKNOWLEDGED: 'bg-indigo-950/70 text-indigo-300 border-indigo-800',
  IN_REVIEW: 'bg-amber-950/70 text-amber-300 border-amber-800',
  CONVERTED_TO_WORK_ORDER: 'bg-emerald-950/70 text-emerald-300 border-emerald-800',
  CANCELLED: 'bg-slate-800 text-slate-300 border-slate-600',
  REJECTED: 'bg-rose-950/70 text-rose-300 border-rose-800',
};

const priorityStyles: Record<WorkOrderPriority, string> = {
  LOW: 'bg-slate-800 text-slate-300 border-slate-600',
  MEDIUM: 'bg-sky-950/70 text-sky-300 border-sky-800',
  HIGH: 'bg-orange-950/70 text-orange-300 border-orange-800',
  URGENT: 'bg-rose-950/70 text-rose-300 border-rose-800',
};

const badgeClass = (extra: string) =>
  `inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border ${extra}`;

export const ServiceRequestStatusBadge: React.FC<{ status: ServiceRequestStatus }> = ({ status }) => (
  <span className={badgeClass(statusStyles[status])}>{status.replaceAll('_', ' ')}</span>
);

export const ServiceRequestPriorityBadge: React.FC<{ priority: WorkOrderPriority }> = ({ priority }) => (
  <span className={badgeClass(priorityStyles[priority])}>{priority}</span>
);

export const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
};

export const formatDate = (value?: string | null) => {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString();
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
