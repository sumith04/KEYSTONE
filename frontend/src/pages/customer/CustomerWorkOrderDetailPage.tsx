import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { formatDateTime } from '../../components/ServiceRequestBadges';
import {
  formatWorkType,
  SlaStatusBadge,
  WorkOrderPriorityBadge,
  WorkOrderStatusBadge,
} from '../work-orders/WorkOrderBadges';
import { getCustomerWorkOrder } from '../../services/api';
import { CustomerWorkOrder, WorkOrderStatus } from '../../types';
import { getApiError } from '../../utils/apiError';

const lifecycle: WorkOrderStatus[] = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CLOSED', 'CANCELLED'];

export const CustomerWorkOrderDetailPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const workOrderId = Number(id);
  const [workOrder, setWorkOrder] = useState<CustomerWorkOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(workOrderId)) {
      setError('Invalid work order id');
      setLoading(false);
      return;
    }
    getCustomerWorkOrder(workOrderId)
      .then(setWorkOrder)
      .catch((err) => setError(getApiError(err).message || 'Unable to load work order'))
      .finally(() => setLoading(false));
  }, [workOrderId]);

  if (loading) {
    return <p className="text-sm text-slate-400">Loading work order...</p>;
  }

  if (error && !workOrder) {
    return <AlertBanner tone="error" message={error} />;
  }

  if (!workOrder) {
    return <AlertBanner tone="error" message="Work order not found." />;
  }

  const timeline = [
    { label: 'Created', value: workOrder.createdAt },
    { label: 'Scheduled start', value: workOrder.scheduledStart },
    { label: 'Actual start', value: workOrder.actualStart },
    { label: 'Actual end', value: workOrder.actualEnd },
    { label: 'Updated', value: workOrder.updatedAt },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wider text-slate-500">Work order</p>
          <h1 className="text-2xl font-bold text-white">{workOrder.title}</h1>
          <p className="text-sm text-slate-400">{workOrder.workOrderNumber}</p>
        </div>
        <button
          type="button"
          onClick={() => navigate('/customer/work-orders')}
          className="px-3 py-2 rounded-lg border border-slate-700 text-sm text-slate-200 hover:bg-slate-800"
        >
          Back to work orders
        </button>
      </div>

      <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            <WorkOrderStatusBadge status={workOrder.status} />
            <WorkOrderPriorityBadge priority={workOrder.priority} />
            <SlaStatusBadge status={workOrder.slaStatus} />
          </div>
          <p className="text-sm text-slate-300 whitespace-pre-wrap">{workOrder.description || 'No description provided.'}</p>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Site</dt>
              <dd className="text-slate-200">{workOrder.siteName || '—'} {workOrder.siteCode ? `(${workOrder.siteCode})` : ''}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Work type</dt>
              <dd className="text-slate-200">{formatWorkType(workOrder.workType)}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Technician</dt>
              <dd className="text-slate-200">{workOrder.assignedTechnicianName || 'Not assigned yet'}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Service request</dt>
              <dd className="text-slate-200">
                {workOrder.serviceRequestId ? (
                  <Link to={`/customer/requests/${workOrder.serviceRequestId}`} className="text-brand-300 hover:text-brand-200">
                    {workOrder.serviceRequestNumber}
                  </Link>
                ) : (
                  '—'
                )}
              </dd>
            </div>
          </dl>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4 text-sm">
          <h2 className="text-sm font-semibold text-white">SLA</h2>
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Response due</p>
            <p className="text-slate-200">{formatDateTime(workOrder.responseDueAt)}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Responded</p>
            <p className="text-slate-200">{formatDateTime(workOrder.responseAt)}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Resolution due</p>
            <p className="text-slate-200">{formatDateTime(workOrder.resolutionDueAt)}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Resolved</p>
            <p className="text-slate-200">{formatDateTime(workOrder.resolvedAt)}</p>
          </div>
        </div>
      </section>

      <section className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
        <h2 className="text-sm font-semibold text-white">Lifecycle</h2>
        <div className="flex flex-wrap gap-2">
          {lifecycle.map((status) => (
            <span
              key={status}
              className={`px-2.5 py-1 rounded-full text-[11px] border ${
                workOrder.status === status
                  ? 'bg-brand-950 text-brand-200 border-brand-700'
                  : 'bg-slate-950 text-slate-500 border-slate-800'
              }`}
            >
              {status.replace('_', ' ')}
            </span>
          ))}
        </div>
        <ol className="space-y-3">
          {timeline.map((item) => (
            <li key={item.label} className="flex items-start justify-between gap-4 text-sm border-b border-slate-800 pb-3 last:border-0 last:pb-0">
              <span className="text-slate-400">{item.label}</span>
              <span className="text-slate-200">{formatDateTime(item.value)}</span>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
};
