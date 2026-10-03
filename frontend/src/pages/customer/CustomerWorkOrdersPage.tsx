import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { Pagination } from '../../components/Pagination';
import { formatDateTime } from '../../components/ServiceRequestBadges';
import { SlaStatusBadge, WorkOrderPriorityBadge, WorkOrderStatusBadge } from '../work-orders/WorkOrderBadges';
import { getCustomerWorkOrders } from '../../services/api';
import { CustomerWorkOrderPage, WorkOrderStatus } from '../../types';
import { getApiError } from '../../utils/apiError';

export const CustomerWorkOrdersPage: React.FC = () => {
  const navigate = useNavigate();
  const [pageData, setPageData] = useState<CustomerWorkOrderPage | null>(null);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<WorkOrderStatus | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        setPageData(
          await getCustomerWorkOrders({
            page,
            size: 10,
            search,
            status: status || undefined,
            sort: 'createdAt,desc',
          })
        );
      } catch (err) {
        setError(getApiError(err).message || 'Unable to load work orders');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [page, search, status]);

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">My Work Orders</h1>
        <p className="text-sm text-slate-400">Track work created from your service requests.</p>
      </div>

      <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-3 bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="md:col-span-2 relative">
          <Search className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search work order number or title"
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
          <option value="NEW">New</option>
          <option value="ASSIGNED">Assigned</option>
          <option value="IN_PROGRESS">In progress</option>
          <option value="ON_HOLD">On hold</option>
          <option value="COMPLETED">Completed</option>
          <option value="CLOSED">Closed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <button type="submit" className="rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-4 py-2 text-sm">
          Search
        </button>
      </form>

      {error && <AlertBanner tone="error" message={error} />}

      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-400">Loading work orders...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-6 text-sm text-slate-400">No work orders match the current filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/70 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Work order</th>
                  <th className="px-4 py-3">Site</th>
                  <th className="px-4 py-3">Priority</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">SLA</th>
                  <th className="px-4 py-3">Scheduled</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((workOrder) => (
                  <tr
                    key={workOrder.id}
                    onClick={() => navigate(`/customer/work-orders/${workOrder.id}`)}
                    className="border-t border-slate-800 hover:bg-slate-800/40 cursor-pointer"
                  >
                    <td className="px-4 py-3">
                      <p className="font-medium text-white">{workOrder.title}</p>
                      <p className="text-xs text-slate-500">{workOrder.workOrderNumber}</p>
                    </td>
                    <td className="px-4 py-3 text-slate-300">{workOrder.siteName || '—'}</td>
                    <td className="px-4 py-3">
                      <WorkOrderPriorityBadge priority={workOrder.priority} />
                    </td>
                    <td className="px-4 py-3">
                      <WorkOrderStatusBadge status={workOrder.status} />
                    </td>
                    <td className="px-4 py-3">
                      <SlaStatusBadge status={workOrder.slaStatus} />
                    </td>
                    <td className="px-4 py-3 text-slate-400">{formatDateTime(workOrder.scheduledStart)}</td>
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
