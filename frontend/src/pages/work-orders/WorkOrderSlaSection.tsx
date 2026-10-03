import React, { useEffect, useState } from 'react';
import { AlertBanner } from '../../components/AlertBanner';
import { getWorkOrderSla } from '../../services/api';
import { WorkOrder, WorkOrderSla } from '../../types';
import { getApiError } from '../../utils/apiError';
import { formatDateTime, SlaStatusBadge } from './WorkOrderBadges';

const formatRemaining = (minutes: number | null) => {
  if (minutes == null) {
    return '—';
  }
  const abs = Math.abs(minutes);
  const hours = Math.floor(abs / 60);
  const remaining = abs % 60;
  const label = hours === 0 ? `${remaining} min` : `${hours}h ${remaining}m`;
  return minutes < 0 ? `${label} overdue` : label;
};

interface WorkOrderSlaSectionProps {
  workOrder: WorkOrder;
}

export const WorkOrderSlaSection: React.FC<WorkOrderSlaSectionProps> = ({ workOrder }) => {
  const [sla, setSla] = useState<WorkOrderSla | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        setSla(await getWorkOrderSla(workOrder.id));
      } catch (err) {
        setError(getApiError(err).message || 'Unable to load SLA details');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [workOrder.id, workOrder.status, workOrder.assignedTechnicianId]);

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">SLA</h2>
      {error && <AlertBanner tone="error" message={error} />}
      {loading ? (
        <div className="text-sm text-slate-400">Loading SLA...</div>
      ) : !sla ? (
        <div className="text-sm text-slate-400">No SLA information is available.</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <Detail label="Policy" value={sla.slaPolicyName || workOrder.slaPolicyName} />
          <Detail label="Status" value={<SlaStatusBadge status={sla.slaStatus} />} />
          <Detail label="Response deadline" value={formatDateTime(sla.responseDueAt)} />
          <Detail label="Responded at" value={formatDateTime(sla.responseAt)} />
          <Detail label="Response breach" value={formatBoolean(sla.responseBreached)} />
          <Detail label="Remaining response" value={formatRemaining(sla.remainingResponseMinutes)} />
          <Detail label="Resolution deadline" value={formatDateTime(sla.resolutionDueAt)} />
          <Detail label="Resolved at" value={formatDateTime(sla.resolvedAt)} />
          <Detail label="Resolution breach" value={formatBoolean(sla.resolutionBreached)} />
          <Detail label="Remaining resolution" value={formatRemaining(sla.remainingResolutionMinutes)} />
        </div>
      )}
    </div>
  );
};

const formatBoolean = (value: boolean | null) => {
  if (value == null) {
    return '—';
  }
  return value ? 'Yes' : 'No';
};

const Detail: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div>
    <p className="text-xs uppercase tracking-wider text-slate-500 mb-1">{label}</p>
    <div className="text-slate-200">{value || '—'}</div>
  </div>
);
