import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ClipboardList, CheckCircle2, Inbox, Wrench } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { ServiceRequestStatusBadge, formatDateTime } from '../../components/ServiceRequestBadges';
import { SlaStatusBadge, WorkOrderStatusBadge } from '../work-orders/WorkOrderBadges';
import {
  getCustomerPortalSummary,
  getCustomerWorkOrders,
  getMyServiceRequests,
} from '../../services/api';
import { CustomerPortalSummary, CustomerWorkOrder, ServiceRequest } from '../../types';
import { getApiError } from '../../utils/apiError';

export const CustomerDashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<CustomerPortalSummary | null>(null);
  const [requests, setRequests] = useState<ServiceRequest[]>([]);
  const [workOrders, setWorkOrders] = useState<CustomerWorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const [summaryData, requestData, workOrderData] = await Promise.all([
          getCustomerPortalSummary(),
          getMyServiceRequests({ page: 0, size: 5, sort: 'createdAt,desc' }),
          getCustomerWorkOrders({ page: 0, size: 5, sort: 'createdAt,desc' }),
        ]);
        setSummary(summaryData);
        setRequests(requestData.content);
        setWorkOrders(workOrderData.content);
      } catch (err) {
        setError(getApiError(err).message || 'Unable to load the customer dashboard');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const cards = [
    { label: 'Submitted requests', value: summary?.submittedRequests ?? 0, icon: Inbox },
    { label: 'Open requests', value: summary?.openRequests ?? 0, icon: ClipboardList },
    { label: 'Active work orders', value: summary?.activeWorkOrders ?? 0, icon: Wrench },
    { label: 'Completed work orders', value: summary?.completedWorkOrders ?? 0, icon: CheckCircle2 },
    { label: 'SLA warnings', value: summary?.slaWarnings ?? 0, icon: AlertTriangle },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Customer Dashboard</h1>
          <p className="text-sm text-slate-400">Track your service requests, work orders, and SLA status.</p>
        </div>
        <Link
          to="/customer/requests/new"
          className="inline-flex items-center justify-center px-4 py-2 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm"
        >
          Raise a request
        </Link>
      </div>

      {error && <AlertBanner tone="error" message={error} />}

      <section className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4">
        {cards.map((card) => (
          <div key={card.label} className="bg-slate-900 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center justify-between">
              <p className="text-xs uppercase tracking-wide text-slate-500">{card.label}</p>
              <card.icon className="w-4 h-4 text-slate-500" />
            </div>
            <p className="mt-3 text-2xl font-semibold text-white">{loading ? '—' : card.value}</p>
          </div>
        ))}
      </section>

      <section className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-white">Recent requests</h2>
            <Link to="/customer/requests" className="text-xs text-brand-300 hover:text-brand-200">
              View all
            </Link>
          </div>
          {loading ? (
            <p className="text-sm text-slate-500">Loading requests...</p>
          ) : requests.length === 0 ? (
            <p className="text-sm text-slate-500">No service requests yet.</p>
          ) : (
            <div className="space-y-3">
              {requests.map((request) => (
                <Link
                  key={request.id}
                  to={`/customer/requests/${request.id}`}
                  className="block rounded-lg border border-slate-800 px-3 py-3 hover:bg-slate-800/50"
                >
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-medium text-white truncate">{request.title}</p>
                    <ServiceRequestStatusBadge status={request.status} />
                  </div>
                  <p className="mt-1 text-xs text-slate-500">
                    {request.requestNumber} · {formatDateTime(request.createdAt)}
                  </p>
                </Link>
              ))}
            </div>
          )}
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-white">Recent work orders</h2>
            <Link to="/customer/work-orders" className="text-xs text-brand-300 hover:text-brand-200">
              View all
            </Link>
          </div>
          {loading ? (
            <p className="text-sm text-slate-500">Loading work orders...</p>
          ) : workOrders.length === 0 ? (
            <p className="text-sm text-slate-500">No work orders yet.</p>
          ) : (
            <div className="space-y-3">
              {workOrders.map((workOrder) => (
                <Link
                  key={workOrder.id}
                  to={`/customer/work-orders/${workOrder.id}`}
                  className="block rounded-lg border border-slate-800 px-3 py-3 hover:bg-slate-800/50"
                >
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-medium text-white truncate">{workOrder.title}</p>
                    <WorkOrderStatusBadge status={workOrder.status} />
                  </div>
                  <div className="mt-1 flex items-center justify-between text-xs text-slate-500">
                    <span>
                      {workOrder.workOrderNumber} · {formatDateTime(workOrder.createdAt)}
                    </span>
                    <SlaStatusBadge status={workOrder.slaStatus} />
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
};
