import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Search } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Pagination } from '../../components/Pagination';
import { StatusBadge } from '../../components/StatusBadge';
import { usePermissions } from '../../hooks/usePermissions';
import {
  deleteCustomer,
  getCustomers,
  updateCustomerStatus,
} from '../../services/api';
import { Customer, CustomerPageResponse, CustomerStatus } from '../../types';
import { getApiError } from '../../utils/apiError';

export const CustomerListPage: React.FC = () => {
  const navigate = useNavigate();
  const { hasPermission } = usePermissions();
  const [pageData, setPageData] = useState<CustomerPageResponse | null>(null);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<CustomerStatus | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Customer | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const canCreate = hasPermission('CREATE_CUSTOMER');
  const canUpdate = hasPermission('UPDATE_CUSTOMER');
  const canDelete = hasPermission('DELETE_CUSTOMER');

  const loadCustomers = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getCustomers({
        page,
        size: 10,
        search,
        status: status || undefined,
      });
      setPageData(data);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load customers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCustomers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, search, status]);

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  const handleToggleStatus = async (customer: Customer) => {
    const nextStatus: CustomerStatus = customer.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setError(null);
    try {
      await updateCustomerStatus(customer.id, nextStatus);
      setSuccess(`Customer ${customer.customerCode} is now ${nextStatus}.`);
      await loadCustomers();
    } catch (err) {
      setError(getApiError(err).message || 'Unable to update customer status');
    }
  };

  const handleDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteCustomer(pendingDelete.id);
      setSuccess(`Customer ${pendingDelete.customerCode} was deleted.`);
      setPendingDelete(null);
      await loadCustomers();
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to delete customer');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Customers</h1>
          <p className="text-sm text-slate-400">Manage commercial customer accounts and contacts.</p>
        </div>
        {canCreate && (
          <Link
            to="/customers/new"
            className="inline-flex items-center justify-center space-x-2 px-4 py-2 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Create customer</span>
          </Link>
        )}
      </div>

      <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-3 bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="md:col-span-2 relative">
          <Search className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search code, company, email, phone, or city"
            className="w-full rounded-lg border border-slate-700 bg-slate-950 pl-9 pr-3 py-2 text-sm"
          />
        </div>
        <select
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value as CustomerStatus | '');
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
          <div className="p-6 text-sm text-slate-400">Loading customers...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-6 text-sm text-slate-400">No customers match the current filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/80 text-slate-400">
                <tr>
                  <th className="text-left px-4 py-3 font-medium">Code</th>
                  <th className="text-left px-4 py-3 font-medium">Company</th>
                  <th className="text-left px-4 py-3 font-medium">Email</th>
                  <th className="text-left px-4 py-3 font-medium">City</th>
                  <th className="text-left px-4 py-3 font-medium">Status</th>
                  <th className="text-right px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((customer) => (
                  <tr key={customer.id} className="border-t border-slate-800">
                    <td className="px-4 py-3 font-medium text-slate-100">{customer.customerCode}</td>
                    <td className="px-4 py-3">{customer.companyName}</td>
                    <td className="px-4 py-3 text-slate-400">{customer.email || '—'}</td>
                    <td className="px-4 py-3 text-slate-400">{customer.city || '—'}</td>
                    <td className="px-4 py-3">
                      <StatusBadge status={customer.status} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => navigate(`/customers/${customer.id}`)}
                          className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                        >
                          View
                        </button>
                        {canUpdate && (
                          <>
                            <button
                              type="button"
                              onClick={() => navigate(`/customers/${customer.id}/edit`)}
                              className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              onClick={() => handleToggleStatus(customer)}
                              className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                            >
                              {customer.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                            </button>
                          </>
                        )}
                        {canDelete && (
                          <button
                            type="button"
                            onClick={() => {
                              setDeleteError(null);
                              setPendingDelete(customer);
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
        title="Delete customer"
        message={
          pendingDelete
            ? `Delete ${pendingDelete.companyName} (${pendingDelete.customerCode})? This is blocked if the customer still has sites.`
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
