import React, { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { ClipboardList, PauseCircle, PlayCircle, Search, CheckCircle2 } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { Pagination } from '../../components/Pagination';
import { getWorkOrderSummary, getWorkOrders } from '../../services/api';
import { WorkOrderPageResponse, WorkOrderPriority, WorkOrderStatus, WorkOrderSummary } from '../../types';
import { getApiError } from '../../utils/apiError';
import {
  formatDateTime,
  WorkOrderPriorityBadge,
  WorkOrderStatusBadge,
} from '../work-orders/WorkOrderBadges';

export const TechnicianWorkspacePage: React.FC = () => {
  const navigate = useNavigate();
  const { role } = useAuth();
  const [pageData, setPageData] = useState<WorkOrderPageResponse | null>(null);
  const [summary, setSummary] = useState<WorkOrderSummary | null>(null);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<WorkOrderStatus | ''>('');
  const [priority, setPriority] = useState<WorkOrderPriority | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadWorkspace = async () => {
    setLoading(true);
    setError(null);
    try {
      const [workOrders, counts] = await Promise.all([
        getWorkOrders({
          page,
          size: 10,
          search,
          status: status || undefined,
          priority: priority || undefined,
          sort: 'createdAt,desc',
        }),
        getWorkOrderSummary(),
      ]);
      setPageData(workOrders);
      setSummary(counts);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load technician workspace');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (role && role !== 'TECHNICIAN') {
      return;
    }
    loadWorkspace();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, search, status, priority, role]);

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  if (role && role !== 'TECHNICIAN') {
    return <Navigate to="/work-orders" replace />;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">My Work Orders</h1>
        <p className="text-sm text-slate-400">Assigned field work you can start, hold, resume, or complete.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
        <SummaryCard
          label="New / Assigned"
          value={summary?.assigned}
          icon={<ClipboardList className="w-4 h-4" />}
          tone="sky"
        />
        <SummaryCard
          label="In Progress"
          value={summary?.inProgress}
          icon={<PlayCircle className="w-4 h-4" />}
          tone="indigo"
        />
        <SummaryCard
          label="On Hold"
          value={summary?.onHold}
          icon={<PauseCircle className="w-4 h-4" />}
          tone="amber"
        />
        <SummaryCard
          label="Completed"
          value={summary?.completed}
          icon={<CheckCircle2 className="w-4 h-4" />}
          tone="emerald"
        />
      </div>

      <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-3 bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="md:col-span-2 relative">
          <Search className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search number, title, or description"
            className="w-full rounded-lg border border-slate-700 bg-slate-950 pl-9 pr-3 py-2 text-sm"
          />
        </div>
        <select
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value as WorkOrderStatus | '');
          }}
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
        >
          <option value="">All statuses</option>
          <option value="ASSIGNED">Assigned</option>
          <option value="IN_PROGRESS">In progress</option>
          <option value="ON_HOLD">On hold</option>
          <option value="COMPLETED">Completed</option>
          <option value="CLOSED">Closed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-1 gap-3">
          <select
            value={priority}
            onChange={(event) => {
              setPage(0);
              setPriority(event.target.value as WorkOrderPriority | '');
            }}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
          >
            <option value="">All priorities</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </select>
          <button type="submit" className="rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-4 py-2 text-sm md:hidden">
            Search
          </button>
        </div>
        <button type="submit" className="hidden md:inline-flex md:col-span-4 justify-center rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-4 py-2 text-sm">
          Search
        </button>
      </form>

      {error && <AlertBanner tone="error" message={error} />}

      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-400">Loading your work orders...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-6 text-sm text-slate-400">No assigned work orders match the current filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/80 text-slate-400">
                <tr>
                  <th className="text-left px-4 py-3 font-medium">Number</th>
                  <th className="text-left px-4 py-3 font-medium">Title</th>
                  <th className="text-left px-4 py-3 font-medium">Customer</th>
                  <th className="text-left px-4 py-3 font-medium">Site</th>
                  <th className="text-left px-4 py-3 font-medium">Priority</th>
                  <th className="text-left px-4 py-3 font-medium">Status</th>
                  <th className="text-left px-4 py-3 font-medium">Scheduled start</th>
                  <th className="text-left px-4 py-3 font-medium">Scheduled end</th>
                  <th className="text-right px-4 py-3 font-medium">Action</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((workOrder) => (
                  <tr key={workOrder.id} className="border-t border-slate-800">
                    <td className="px-4 py-3 font-medium text-slate-100">{workOrder.workOrderNumber}</td>
                    <td className="px-4 py-3">{workOrder.title}</td>
                    <td className="px-4 py-3 text-slate-400">{workOrder.customerName}</td>
                    <td className="px-4 py-3 text-slate-400">{workOrder.siteName}</td>
                    <td className="px-4 py-3">
                      <WorkOrderPriorityBadge priority={workOrder.priority} />
                    </td>
                    <td className="px-4 py-3">
                      <WorkOrderStatusBadge status={workOrder.status} />
                    </td>
                    <td className="px-4 py-3 text-slate-400">{formatDateTime(workOrder.scheduledStart)}</td>
                    <td className="px-4 py-3 text-slate-400">{formatDateTime(workOrder.scheduledEnd)}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        type="button"
                        onClick={() => navigate(`/work-orders/${workOrder.id}`)}
                        className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                      >
                        Open
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {pageData && (
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onPageChange={setPage}
        />
      )}
    </div>
  );
};

const SummaryCard: React.FC<{
  label: string;
  value?: number;
  icon: React.ReactNode;
  tone: 'sky' | 'indigo' | 'amber' | 'emerald';
}> = ({ label, value, icon, tone }) => {
  const tones = {
    sky: 'text-sky-300 border-sky-800 bg-sky-950/40',
    indigo: 'text-indigo-300 border-indigo-800 bg-indigo-950/40',
    amber: 'text-amber-300 border-amber-800 bg-amber-950/40',
    emerald: 'text-emerald-300 border-emerald-800 bg-emerald-950/40',
  };

  return (
    <div className={`rounded-xl border p-4 ${tones[tone]}`}>
      <div className="flex items-center justify-between text-xs uppercase tracking-wider opacity-80">
        <span>{label}</span>
        {icon}
      </div>
      <p className="mt-2 text-2xl font-bold text-white">{value ?? '—'}</p>
    </div>
  );
};
