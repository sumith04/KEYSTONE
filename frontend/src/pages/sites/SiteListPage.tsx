import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Search } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Pagination } from '../../components/Pagination';
import { StatusBadge } from '../../components/StatusBadge';
import { usePermissions } from '../../hooks/usePermissions';
import { deleteSite, getCustomers, getSites, updateSiteStatus } from '../../services/api';
import { Customer, Site, SitePageResponse, SiteStatus } from '../../types';
import { getApiError } from '../../utils/apiError';

export const SiteListPage: React.FC = () => {
  const navigate = useNavigate();
  const { hasPermission } = usePermissions();
  const [pageData, setPageData] = useState<SitePageResponse | null>(null);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<SiteStatus | ''>('');
  const [customerId, setCustomerId] = useState<number | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Site | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const canCreate = hasPermission('CREATE_SITE');
  const canUpdate = hasPermission('UPDATE_SITE');
  const canDelete = hasPermission('DELETE_SITE');
  const canViewCustomers = hasPermission('VIEW_CUSTOMER');

  const loadSites = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getSites({
        page,
        size: 10,
        search,
        status: status || undefined,
        customerId: customerId === '' ? undefined : customerId,
      });
      setPageData(data);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load sites');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSites();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, search, status, customerId]);

  useEffect(() => {
    if (!canViewCustomers) {
      return;
    }
    getCustomers({ page: 0, size: 100, sort: 'companyName' })
      .then((data) => setCustomers(data.content))
      .catch(() => {
        // Customer filter is optional for roles that can view sites only.
      });
  }, [canViewCustomers]);

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  const handleToggleStatus = async (site: Site) => {
    const nextStatus: SiteStatus = site.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await updateSiteStatus(site.id, nextStatus);
      setSuccess(`Site ${site.siteCode} is now ${nextStatus}.`);
      await loadSites();
    } catch (err) {
      setError(getApiError(err).message || 'Unable to update site status');
    }
  };

  const handleDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteSite(pendingDelete.id);
      setSuccess(`Site ${pendingDelete.siteCode} was deleted.`);
      setPendingDelete(null);
      await loadSites();
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to delete site');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Sites</h1>
          <p className="text-sm text-slate-400">Manage customer facilities and site contacts.</p>
        </div>
        {canCreate && (
          <Link
            to="/sites/new"
            className="inline-flex items-center justify-center space-x-2 px-4 py-2 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Create site</span>
          </Link>
        )}
      </div>

      <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-5 gap-3 bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="md:col-span-2 relative">
          <Search className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search code, name, contact, email, or city"
            className="w-full rounded-lg border border-slate-700 bg-slate-950 pl-9 pr-3 py-2 text-sm"
          />
        </div>
        <select
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value as SiteStatus | '');
          }}
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
        >
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>
        {canViewCustomers ? (
          <select
            value={customerId}
            onChange={(event) => {
              setPage(0);
              setCustomerId(event.target.value ? Number(event.target.value) : '');
            }}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
          >
            <option value="">All customers</option>
            {customers.map((customer) => (
              <option key={customer.id} value={customer.id}>
                {customer.companyName}
              </option>
            ))}
          </select>
        ) : (
          <div className="rounded-lg border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-500">
            Customer filter unavailable
          </div>
        )}
        <button type="submit" className="rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-4 py-2 text-sm">
          Search
        </button>
      </form>

      {success && <AlertBanner tone="success" message={success} />}
      {error && <AlertBanner tone="error" message={error} />}

      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-400">Loading sites...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-6 text-sm text-slate-400">No sites match the current filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/80 text-slate-400">
                <tr>
                  <th className="text-left px-4 py-3 font-medium">Code</th>
                  <th className="text-left px-4 py-3 font-medium">Site</th>
                  <th className="text-left px-4 py-3 font-medium">Customer</th>
                  <th className="text-left px-4 py-3 font-medium">City</th>
                  <th className="text-left px-4 py-3 font-medium">Status</th>
                  <th className="text-right px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((site) => (
                  <tr key={site.id} className="border-t border-slate-800">
                    <td className="px-4 py-3 font-medium text-slate-100">{site.siteCode}</td>
                    <td className="px-4 py-3">{site.siteName}</td>
                    <td className="px-4 py-3 text-slate-400">{site.customerName}</td>
                    <td className="px-4 py-3 text-slate-400">{site.city || '—'}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={site.status} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => navigate(`/sites/${site.id}`)}
                          className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                        >
                          View
                        </button>
                        {canUpdate && (
                          <>
                            <button
                              type="button"
                              onClick={() => navigate(`/sites/${site.id}/edit`)}
                              className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              onClick={() => handleToggleStatus(site)}
                              className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                            >
                              {site.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                            </button>
                          </>
                        )}
                        {canDelete && (
                          <button
                            type="button"
                            onClick={() => {
                              setDeleteError(null);
                              setPendingDelete(site);
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
        title="Delete site"
        message={pendingDelete ? `Delete ${pendingDelete.siteName} (${pendingDelete.siteCode})?` : ''}
        busy={deleting}
        error={deleteError}
        onCancel={() => setPendingDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
};
