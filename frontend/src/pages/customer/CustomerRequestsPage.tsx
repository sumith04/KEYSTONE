import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Search } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { Pagination } from '../../components/Pagination';
import { ServiceRequestPriorityBadge, ServiceRequestStatusBadge, formatDateTime } from '../../components/ServiceRequestBadges';
import { getMyServiceRequests } from '../../services/api';
import { ServiceRequestPage, ServiceRequestStatus, WorkOrderPriority } from '../../types';
import { getApiError } from '../../utils/apiError';

export const CustomerRequestsPage: React.FC = () => {
  const navigate = useNavigate();
  const [pageData, setPageData] = useState<ServiceRequestPage | null>(null);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<ServiceRequestStatus | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadRequests = async () => {
    setLoading(true);
    setError(null);
    try {
      setPageData(
        await getMyServiceRequests({
          page,
          size: 10,
          search,
          status: status || undefined,
          sort: 'createdAt,desc',
        })
      );
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load service requests');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, search, status]);

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">My Requests</h1>
          <p className="text-sm text-slate-400">View and raise service requests for your sites.</p>
        </div>
        <Link
          to="/customer/requests/new"
          className="inline-flex items-center justify-center space-x-2 px-4 py-2 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm"
        >
          <Plus className="w-4 h-4" />
          <span>New request</span>
        </Link>
      </div>

      <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-3 bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="md:col-span-2 relative">
          <Search className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search request number or title"
            className="w-full rounded-lg border border-slate-700 bg-slate-950 pl-9 pr-3 py-2 text-sm"
          />
        </div>
        <select
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value as ServiceRequestStatus | '');
          }}
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
        >
          <option value="">All statuses</option>
          <option value="SUBMITTED">Submitted</option>
          <option value="ACKNOWLEDGED">Acknowledged</option>
          <option value="IN_REVIEW">In review</option>
          <option value="CONVERTED_TO_WORK_ORDER">Converted</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="REJECTED">Rejected</option>
        </select>
        <button type="submit" className="rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-4 py-2 text-sm">
          Search
        </button>
      </form>

      {error && <AlertBanner tone="error" message={error} />}

      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-400">Loading requests...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-6 text-sm text-slate-400">No service requests match the current filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/70 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Request</th>
                  <th className="px-4 py-3">Site</th>
                  <th className="px-4 py-3">Priority</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Created</th>
                  <th className="px-4 py-3">Work order</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((request) => (
                  <tr
                    key={request.id}
                    onClick={() => navigate(`/customer/requests/${request.id}`)}
                    className="border-t border-slate-800 hover:bg-slate-800/40 cursor-pointer"
                  >
                    <td className="px-4 py-3">
                      <p className="font-medium text-white">{request.title}</p>
                      <p className="text-xs text-slate-500">{request.requestNumber}</p>
                    </td>
                    <td className="px-4 py-3 text-slate-300">{request.siteName || '—'}</td>
                    <td className="px-4 py-3">
                      <ServiceRequestPriorityBadge priority={request.priority as WorkOrderPriority} />
                    </td>
                    <td className="px-4 py-3">
                      <ServiceRequestStatusBadge status={request.status} />
                    </td>
                    <td className="px-4 py-3 text-slate-400">{formatDateTime(request.createdAt)}</td>
                    <td className="px-4 py-3 text-slate-300">{request.workOrderNumber || '—'}</td>
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
