import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Plus, Search } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Pagination } from '../../components/Pagination';
import { StatusBadge } from '../../components/StatusBadge';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';
import { deletePart, getParts } from '../../services/api';
import { Part, PartPageResponse, PartStatus } from '../../types';
import { getApiError } from '../../utils/apiError';

const formatMoney = (value: number | string) => {
  const amount = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(amount)
    ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
    : String(value);
};

export const PartsPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { role } = useAuth();
  const { hasPermission } = usePermissions();
  const [pageData, setPageData] = useState<PartPageResponse | null>(null);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [categoryInput, setCategoryInput] = useState('');
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState<PartStatus | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(
    (location.state as { success?: string } | null)?.success || null
  );
  const [pendingDelete, setPendingDelete] = useState<Part | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const canManageCatalog = role !== 'TECHNICIAN';
  const canCreate = canManageCatalog && hasPermission('ADD_PART');
  const canUpdate = canManageCatalog && hasPermission('UPDATE_PART');
  const canDelete = canManageCatalog && hasPermission('DELETE_PART');

  const loadParts = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getParts({
        page,
        size: 10,
        search,
        category: category || undefined,
        status: status || undefined,
        sort: 'name,asc',
      });
      setPageData(data);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load parts');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadParts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, search, category, status]);

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
    setCategory(categoryInput.trim());
  };

  const handleDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    setDeleting(true);
    setDeleteError(null);
    try {
      await deletePart(pendingDelete.id);
      setSuccess(`Part ${pendingDelete.partNumber} was deleted.`);
      setPendingDelete(null);
      await loadParts();
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to delete part');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Parts</h1>
          <p className="text-sm text-slate-400">Catalog inventory available for work order usage.</p>
        </div>
        {canCreate && (
          <Link
            to="/parts/new"
            className="inline-flex items-center justify-center space-x-2 px-4 py-2 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Create part</span>
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
            placeholder="Search number, name, or description"
            className="w-full rounded-lg border border-slate-700 bg-slate-950 pl-9 pr-3 py-2 text-sm"
          />
        </div>
        <input
          value={categoryInput}
          onChange={(event) => setCategoryInput(event.target.value)}
          placeholder="Category"
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
        />
        <select
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value as PartStatus | '');
          }}
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
        >
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>
        <button type="submit" className="rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-4 py-2 text-sm">
          Search
        </button>
      </form>

      {success && <AlertBanner tone="success" message={success} />}
      {error && <AlertBanner tone="error" message={error} />}

      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-400">Loading parts...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-6 text-sm text-slate-400">No parts match the current filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/80 text-slate-400">
                <tr>
                  <th className="text-left px-4 py-3 font-medium">Part number</th>
                  <th className="text-left px-4 py-3 font-medium">Name</th>
                  <th className="text-left px-4 py-3 font-medium">Category</th>
                  <th className="text-left px-4 py-3 font-medium">Unit cost</th>
                  <th className="text-left px-4 py-3 font-medium">In stock</th>
                  <th className="text-left px-4 py-3 font-medium">Reorder</th>
                  <th className="text-left px-4 py-3 font-medium">Status</th>
                  <th className="text-right px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((part) => (
                  <tr key={part.id} className="border-t border-slate-800">
                    <td className="px-4 py-3 font-medium text-slate-100">{part.partNumber}</td>
                    <td className="px-4 py-3">{part.name}</td>
                    <td className="px-4 py-3 text-slate-400">{part.category || '—'}</td>
                    <td className="px-4 py-3 text-slate-400">{formatMoney(part.unitCost)}</td>
                    <td className="px-4 py-3">{part.quantityInStock}</td>
                    <td className="px-4 py-3 text-slate-400">{part.reorderLevel}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={part.status} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end flex-wrap gap-2">
                        {canUpdate && (
                          <button
                            type="button"
                            onClick={() => navigate(`/parts/${part.id}/edit`)}
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
                              setPendingDelete(part);
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
        title="Delete part"
        message={
          pendingDelete
            ? `Delete ${pendingDelete.name} (${pendingDelete.partNumber})? This is blocked if the part has been used on a work order.`
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
