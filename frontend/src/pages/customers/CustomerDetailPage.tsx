import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { StatusBadge } from '../../components/StatusBadge';
import { usePermissions } from '../../hooks/usePermissions';
import { deleteCustomer, getCustomer, updateCustomerStatus } from '../../services/api';
import { Customer, CustomerStatus } from '../../types';
import { getApiError } from '../../utils/apiError';

export const CustomerDetailPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasPermission } = usePermissions();
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(
    (location.state as { success?: string } | null)?.success || null
  );
  const [pendingDelete, setPendingDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const customerId = Number(id);

  const loadCustomer = async () => {
    setLoading(true);
    setError(null);
    try {
      setCustomer(await getCustomer(customerId));
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load customer');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!Number.isFinite(customerId)) {
      setError('Invalid customer id');
      setLoading(false);
      return;
    }
    loadCustomer();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [customerId]);

  const handleToggleStatus = async () => {
    if (!customer) {
      return;
    }
    const nextStatus: CustomerStatus = customer.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      const updated = await updateCustomerStatus(customer.id, nextStatus);
      setCustomer(updated);
      setSuccess(`Customer status updated to ${nextStatus}.`);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to update status');
    }
  };

  const handleDelete = async () => {
    if (!customer) {
      return;
    }
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteCustomer(customer.id);
      navigate('/customers', { state: { success: `Customer ${customer.customerCode} was deleted.` } });
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to delete customer');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading customer...</div>;
  }

  if (error && !customer) {
    return <AlertBanner tone="error" message={error} />;
  }

  if (!customer) {
    return <AlertBanner tone="error" message="Customer not found." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wider text-slate-500">Customer</p>
          <h1 className="text-2xl font-bold text-white">{customer.companyName}</h1>
          <p className="text-sm text-slate-400">{customer.customerCode}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/customers" className="px-3 py-2 text-sm rounded-lg border border-slate-700 hover:bg-slate-800">
            Back to list
          </Link>
          {hasPermission('UPDATE_CUSTOMER') && (
            <>
              <Link
                to={`/customers/${customer.id}/edit`}
                className="px-3 py-2 text-sm rounded-lg bg-slate-800 border border-slate-700 hover:bg-slate-700"
              >
                Edit
              </Link>
              <button
                type="button"
                onClick={handleToggleStatus}
                className="px-3 py-2 text-sm rounded-lg border border-slate-700 hover:bg-slate-800"
              >
                {customer.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
              </button>
            </>
          )}
          {hasPermission('DELETE_CUSTOMER') && (
            <button
              type="button"
              onClick={() => setPendingDelete(true)}
              className="px-3 py-2 text-sm rounded-lg border border-rose-800 text-rose-300 hover:bg-rose-950/40"
            >
              Delete
            </button>
          )}
        </div>
      </div>

      {success && <AlertBanner tone="success" message={success} />}
      {error && <AlertBanner tone="error" message={error} />}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-900 border border-slate-800 rounded-xl p-6 text-sm">
        <Detail label="Status" value={<StatusBadge status={customer.status} />} />
        <Detail label="Email" value={customer.email} />
        <Detail label="Phone" value={customer.phone} />
        <Detail label="Alternate phone" value={customer.alternatePhone} />
        <Detail
          label="Contact"
          value={[customer.contactFirstName, customer.contactLastName].filter(Boolean).join(' ') || null}
        />
        <Detail label="City" value={customer.city} />
        <Detail label="State" value={customer.state} />
        <Detail label="Postal code" value={customer.postalCode} />
        <Detail label="Country" value={customer.country} />
        <Detail
          label="Address"
          value={[customer.addressLine1, customer.addressLine2].filter(Boolean).join(', ') || null}
        />
        <Detail
          label="SLA policy"
          value={
            customer.slaPolicyName
              ? `${customer.slaPolicyName}${customer.slaPolicyActive === false ? ' (inactive)' : ''}`
              : 'None'
          }
        />
        <div className="md:col-span-2">
          <Detail label="Notes" value={customer.notes} />
        </div>
        <Detail label="Created" value={new Date(customer.createdAt).toLocaleString()} />
        <Detail label="Updated" value={new Date(customer.updatedAt).toLocaleString()} />
      </div>

      <ConfirmDialog
        open={pendingDelete}
        title="Delete customer"
        message={`Delete ${customer.companyName}? This is blocked if the customer still has sites.`}
        busy={deleting}
        error={deleteError}
        onCancel={() => setPendingDelete(false)}
        onConfirm={handleDelete}
      />
    </div>
  );
};

const Detail: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div>
    <p className="text-xs uppercase tracking-wider text-slate-500 mb-1">{label}</p>
    <div className="text-slate-200">{value || '—'}</div>
  </div>
);
