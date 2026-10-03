import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Plus, Search } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Pagination } from '../../components/Pagination';
import { usePermissions } from '../../hooks/usePermissions';
import { deleteSlaPolicy, getSlaPolicies } from '../../services/api';
import { SlaPolicy, SlaPolicyPageResponse, WorkOrderPriority } from '../../types';
import { getApiError } from '../../utils/apiError';
import { WorkOrderPriorityBadge } from '../work-orders/WorkOrderBadges';

export const SlaPoliciesPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { hasPermission } = usePermissions();
  const [pageData, setPageData] = useState<SlaPolicyPageResponse | null>(null);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [priority, setPriority] = useState<WorkOrderPriority | ''>('');
  const [active, setActive] = useState<'' | 'true' | 'false'>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(
    (location.state as { success?: string } | null)?.success || null
  );
  const [pendingDelete, setPendingDelete] = useState<SlaPolicy | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const canCreate = hasPermission('CREATE_SLA');
  const canUpdate = hasPermission('UPDATE_SLA');
  const canDelete = hasPermission('DELETE_SLA');

  const loadPolicies = async () => {
    setLoading(true);
    setError(null);
    try {
      setPageData(
        await getSlaPolicies({
          page,
          size: 10,
          search,
          priority: priority || undefined,
          active: active === '' ? undefined : active === 'true',
          sort: 'name,asc',
        })
      );
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load SLA policies');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPolicies();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, search, priority, active]);

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  const handleDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteSlaPolicy(pendingDelete.id);
      setSuccess(`SLA policy ${pendingDelete.name} was deleted.`);
      setPendingDelete(null);
      await loadPolicies();
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to delete SLA policy');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">SLA policies</h1>
          <p className="text-sm text-slate-400">Define response and resolution targets for customers.</p>
        </div>
        {canCreate && (
          <Link
            to="/sla-policies/new"
            className="inline-flex items-center justify-center space-x-2 px-4 py-2 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Create policy</span>
          </Link>
        )}
      </div>

      <form
        onSubmit={handleSearch}
        className="grid grid-cols-1 md:grid-cols-5 gap-3 bg-slate-900 border border-slate-800 rounded-xl p-4"
      >
        <div className="md:col-span-2 relative">
          <Search className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search name or description"
            className="w-full rounded-lg border border-slate-700 bg-slate-950 pl-9 pr-3 py-2 text-sm"
          />
        </div>
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
        <select
          value={active}
          onChange={(event) => {
            setPage(0);
            setActive(event.target.value as '' | 'true' | 'false');
          }}
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
        >
          <option value="">All states</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select>
        <button type="submit" className="rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-4 py-2 text-sm">
          Search
        </button>
      </form>

      {success && <AlertBanner tone="success" message={success} />}
      {error && <AlertBanner tone="error" message={error} />}

      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-400">Loading SLA policies...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-6 text-sm text-slate-400">No SLA policies match the current filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/80 text-slate-400">
                <tr>
                  <th className="text-left px-4 py-3 font-medium">Name</th>
                  <th className="text-left px-4 py-3 font-medium">Priority</th>
                  <th className="text-left px-4 py-3 font-medium">Response</th>
                  <th className="text-left px-4 py-3 font-medium">Resolution</th>
                  <th className="text-left px-4 py-3 font-medium">Active</th>
                  <th className="text-right px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((policy) => (
                  <tr key={policy.id} className="border-t border-slate-800">
                    <td className="px-4 py-3 font-medium text-slate-100">{policy.name}</td>
                    <td className="px-4 py-3">
                      <WorkOrderPriorityBadge priority={policy.priority} />
                    </td>
                    <td className="px-4 py-3 text-slate-400">{policy.responseTimeMinutes} min</td>
                    <td className="px-4 py-3 text-slate-400">{policy.resolutionTimeMinutes} min</td>
                    <td className="px-4 py-3 text-slate-400">{policy.active ? 'Active' : 'Inactive'}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end flex-wrap gap-2">
                        {canUpdate && (
                          <button
                            type="button"
                            onClick={() => navigate(`/sla-policies/${policy.id}/edit`)}
                            className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                          >
                            Edit
                          </button>
                        )}
                        {canDelete && (
                          <button
                            type="button"
                            onClick={() => {
                              setDeleteError(null);
                              setPendingDelete(policy);
                            }}
                            className="px-2.5 py-1 rounded-md border border-rose-800 text-rose-300 hover:bg-rose-950/50"
                          >
                            Delete
                          </button>
                        )}
                      </div>
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

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Delete SLA policy"
        message={
          pendingDelete
            ? `Delete ${pendingDelete.name}? This is blocked if customers or work orders still reference it.`
            : ''
        }
        busy={deleting}
        error={deleteError}
        onCancel={() => setPendingDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
};
