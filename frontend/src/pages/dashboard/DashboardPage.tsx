import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ClipboardList, Package, Timer, Users, Wrench } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { getDashboardSummary, getRecentDashboardWorkOrders, getWorkOrderTrend } from '../../services/api';
import {
  DashboardSummary,
  RecentWorkOrder,
  TrendInterval,
  WorkOrderTrend,
} from '../../types';
import { getApiError } from '../../utils/apiError';
import {
  formatDateTime,
  SlaStatusBadge,
  WorkOrderPriorityBadge,
  WorkOrderStatusBadge,
} from '../work-orders/WorkOrderBadges';

const fieldClass =
  'w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100';

export const DashboardPage: React.FC = () => {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [appliedFrom, setAppliedFrom] = useState('');
  const [appliedTo, setAppliedTo] = useState('');
  const [interval, setInterval] = useState<TrendInterval>('DAY');
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [recent, setRecent] = useState<RecentWorkOrder[]>([]);
  const [trend, setTrend] = useState<WorkOrderTrend[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadDashboard = async (nextFrom = appliedFrom, nextTo = appliedTo, nextInterval = interval) => {
    setLoading(true);
    setError(null);
    const params = {
      from: nextFrom || undefined,
      to: nextTo || undefined,
    };
    try {
      const [summaryData, recentData, trendData] = await Promise.all([
        getDashboardSummary(params),
        getRecentDashboardWorkOrders({ ...params, limit: 10 }),
        getWorkOrderTrend({ ...params, interval: nextInterval }),
      ]);
      setSummary(summaryData);
      setRecent(recentData);
      setTrend(trendData);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load dashboard');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const applyFilters = (event: React.FormEvent) => {
    event.preventDefault();
    setAppliedFrom(from);
    setAppliedTo(to);
    loadDashboard(from, to, interval);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Operations dashboard</h1>
          <p className="text-sm text-slate-400">
            Work order metrics use <span className="text-slate-300">createdAt</span>. Time metrics use time-log start
            time. Inventory is the current catalog snapshot.
          </p>
        </div>
        <form onSubmit={applyFilters} className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3 w-full lg:w-auto">
          <input type="date" value={from} onChange={(event) => setFrom(event.target.value)} className={fieldClass} />
          <input type="date" value={to} onChange={(event) => setTo(event.target.value)} className={fieldClass} />
          <select
            value={interval}
            onChange={(event) => setInterval(event.target.value as TrendInterval)}
            className={fieldClass}
          >
            <option value="DAY">Daily trend</option>
            <option value="WEEK">Weekly trend</option>
            <option value="MONTH">Monthly trend</option>
          </select>
          <button type="submit" className="rounded-lg bg-brand-600 hover:bg-brand-500 px-4 py-2 text-sm text-white">
            Apply
          </button>
        </form>
      </div>

      {error && <AlertBanner tone="error" message={error} />}

      {loading || !summary ? (
        <div className="text-sm text-slate-400">Loading dashboard...</div>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
            <MetricCard label="Total work orders" value={summary.totalWorkOrders} icon={<ClipboardList className="w-4 h-4" />} tone="slate" />
            <MetricCard label="Open" value={summary.openWorkOrders} icon={<Wrench className="w-4 h-4" />} tone="sky" />
            <MetricCard label="In progress" value={summary.inProgressWorkOrders} icon={<Timer className="w-4 h-4" />} tone="indigo" />
            <MetricCard label="Completed" value={summary.completedWorkOrders} icon={<ClipboardList className="w-4 h-4" />} tone="emerald" />
            <MetricCard label="SLA breached" value={summary.slaBreachedWorkOrders} icon={<AlertTriangle className="w-4 h-4" />} tone="rose" />
            <MetricCard label="SLA at risk" value={summary.slaAtRiskWorkOrders} icon={<AlertTriangle className="w-4 h-4" />} tone="amber" />
            <MetricCard label="Active technicians" value={summary.activeTechnicians} icon={<Users className="w-4 h-4" />} tone="sky" />
            <MetricCard label="Low stock parts" value={summary.lowStockParts} icon={<Package className="w-4 h-4" />} tone="amber" />
          </div>

          <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
            <DistributionCard
              title="Status distribution"
              rows={summary.statusDistribution.map((row) => ({
                label: row.status.replace('_', ' '),
                value: row.count,
              }))}
            />
            <DistributionCard
              title="Priority distribution"
              rows={summary.priorityDistribution.map((row) => ({
                label: row.priority,
                value: row.count,
              }))}
            />
          </div>

          <section className="bg-slate-900 border border-slate-800 rounded-xl p-4 sm:p-6">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400 mb-4">Technician workload</h2>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-950/80 text-slate-400">
                  <tr>
                    <th className="text-left px-3 py-2 font-medium">Technician</th>
                    <th className="text-right px-3 py-2 font-medium">Assigned</th>
                    <th className="text-right px-3 py-2 font-medium">In progress</th>
                    <th className="text-right px-3 py-2 font-medium">Completed</th>
                    <th className="text-right px-3 py-2 font-medium">Overdue SLA</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.technicianWorkload.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="px-3 py-4 text-slate-400">
                        No assigned work orders in this range.
                      </td>
                    </tr>
                  ) : (
                    summary.technicianWorkload.map((row) => (
                      <tr key={row.technicianId} className="border-t border-slate-800">
                        <td className="px-3 py-2 text-slate-100">{row.technicianName}</td>
                        <td className="px-3 py-2 text-right text-slate-300">{row.assignedWorkOrders}</td>
                        <td className="px-3 py-2 text-right text-slate-300">{row.inProgressWorkOrders}</td>
                        <td className="px-3 py-2 text-right text-slate-300">{row.completedWorkOrders}</td>
                        <td className="px-3 py-2 text-right text-slate-300">{row.overdueSlaWorkOrders}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
            <section className="bg-slate-900 border border-slate-800 rounded-xl p-4 sm:p-6 space-y-3">
              <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">SLA performance</h2>
              <p className="text-sm text-slate-300">
                Compliance:{' '}
                {summary.sla.slaCompliancePercentage == null
                  ? '—'
                  : `${summary.sla.slaCompliancePercentage}%`}
              </p>
              <p className="text-xs text-slate-500">
                Resolved within SLA / resolved work orders that had an SLA. Null when none are resolved.
              </p>
              <DistributionCard
                compact
                title=""
                rows={[
                  { label: 'On track', value: summary.sla.onTrack },
                  { label: 'At risk', value: summary.sla.atRisk },
                  { label: 'Breached', value: summary.sla.breached },
                  { label: 'Resolved', value: summary.sla.resolved },
                ]}
              />
            </section>
            <section className="bg-slate-900 border border-slate-800 rounded-xl p-4 sm:p-6 space-y-3 text-sm">
              <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Inventory and time</h2>
              <p className="text-slate-300">Parts: {summary.inventory.totalParts} total, {summary.inventory.activeParts} active</p>
              <p className="text-slate-300">Low stock: {summary.inventory.lowStockParts} · Out of stock: {summary.inventory.outOfStockParts}</p>
              <p className="text-slate-300">
                Logged time: {summary.time.totalLoggedMinutes} min ({summary.time.totalLoggedHours} hr)
              </p>
              <p className="text-slate-300">Technicians with time logs: {summary.time.activeTechniciansWithTimeLogs}</p>
            </section>
          </div>

          <section className="bg-slate-900 border border-slate-800 rounded-xl p-4 sm:p-6">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400 mb-4">Work order trend</h2>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-950/80 text-slate-400">
                  <tr>
                    <th className="text-left px-3 py-2 font-medium">Period</th>
                    <th className="text-right px-3 py-2 font-medium">Created</th>
                    <th className="text-right px-3 py-2 font-medium">Completed</th>
                    <th className="text-right px-3 py-2 font-medium">Closed</th>
                    <th className="px-3 py-2 font-medium">Created volume</th>
                  </tr>
                </thead>
                <tbody>
                  {trend.map((row) => {
                    const max = Math.max(...trend.map((item) => item.created), 1);
                    return (
                      <tr key={row.period} className="border-t border-slate-800">
                        <td className="px-3 py-2 text-slate-100">{row.period}</td>
                        <td className="px-3 py-2 text-right text-slate-300">{row.created}</td>
                        <td className="px-3 py-2 text-right text-slate-300">{row.completed}</td>
                        <td className="px-3 py-2 text-right text-slate-300">{row.closed}</td>
                        <td className="px-3 py-2">
                          <div className="h-2 rounded bg-slate-800 overflow-hidden">
                            <div
                              className="h-full bg-brand-500"
                              style={{ width: `${Math.round((row.created / max) * 100)}%` }}
                            />
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <section className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
            <div className="px-4 sm:px-6 py-4 border-b border-slate-800">
              <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Recent work orders</h2>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="bg-slate-950/80 text-slate-400">
                  <tr>
                    <th className="text-left px-4 py-3 font-medium">Number</th>
                    <th className="text-left px-4 py-3 font-medium">Title</th>
                    <th className="text-left px-4 py-3 font-medium">Status</th>
                    <th className="text-left px-4 py-3 font-medium">Priority</th>
                    <th className="text-left px-4 py-3 font-medium">Customer</th>
                    <th className="text-left px-4 py-3 font-medium">SLA</th>
                    <th className="text-left px-4 py-3 font-medium">Created</th>
                  </tr>
                </thead>
                <tbody>
                  {recent.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-4 py-4 text-slate-400">
                        No recent work orders.
                      </td>
                    </tr>
                  ) : (
                    recent.map((workOrder) => (
                      <tr key={workOrder.id} className="border-t border-slate-800">
                        <td className="px-4 py-3">
                          <Link to={`/work-orders/${workOrder.id}`} className="text-brand-300 hover:text-brand-200">
                            {workOrder.workOrderNumber}
                          </Link>
                        </td>
                        <td className="px-4 py-3 text-slate-200">{workOrder.title}</td>
                        <td className="px-4 py-3">
                          <WorkOrderStatusBadge status={workOrder.status} />
                        </td>
                        <td className="px-4 py-3">
                          <WorkOrderPriorityBadge priority={workOrder.priority} />
                        </td>
                        <td className="px-4 py-3 text-slate-400">{workOrder.customer || '—'}</td>
                        <td className="px-4 py-3">
                          <SlaStatusBadge status={workOrder.slaStatus} />
                        </td>
                        <td className="px-4 py-3 text-slate-400">{formatDateTime(workOrder.createdAt)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  );
};

const MetricCard: React.FC<{
  label: string;
  value: number;
  icon: React.ReactNode;
  tone: 'slate' | 'sky' | 'indigo' | 'emerald' | 'rose' | 'amber';
}> = ({ label, value, icon, tone }) => {
  const tones = {
    slate: 'text-slate-300 border-slate-700 bg-slate-900',
    sky: 'text-sky-300 border-sky-800 bg-sky-950/40',
    indigo: 'text-indigo-300 border-indigo-800 bg-indigo-950/40',
    emerald: 'text-emerald-300 border-emerald-800 bg-emerald-950/40',
    rose: 'text-rose-300 border-rose-800 bg-rose-950/40',
    amber: 'text-amber-300 border-amber-800 bg-amber-950/40',
  };
  return (
    <div className={`rounded-xl border p-4 ${tones[tone]}`}>
      <div className="flex items-center justify-between text-xs uppercase tracking-wider">
        <span>{label}</span>
        {icon}
      </div>
      <p className="mt-2 text-2xl font-bold text-white">{value}</p>
    </div>
  );
};

const DistributionCard: React.FC<{
  title: string;
  rows: Array<{ label: string; value: number }>;
  compact?: boolean;
}> = ({ title, rows, compact }) => {
  const max = Math.max(...rows.map((row) => row.value), 1);
  return (
    <section className={compact ? 'space-y-2' : 'bg-slate-900 border border-slate-800 rounded-xl p-4 sm:p-6 space-y-3'}>
      {title && <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">{title}</h2>}
      {rows.map((row) => (
        <div key={row.label} className="space-y-1">
          <div className="flex justify-between text-xs text-slate-400">
            <span>{row.label}</span>
            <span>{row.value}</span>
          </div>
          <div className="h-2 rounded bg-slate-800 overflow-hidden">
            <div className="h-full bg-brand-500" style={{ width: `${Math.round((row.value / max) * 100)}%` }} />
          </div>
        </div>
      ))}
    </section>
  );
};
