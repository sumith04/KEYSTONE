import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { StatusBadge } from '../../components/StatusBadge';
import { usePermissions } from '../../hooks/usePermissions';
import { deleteSite, getSite, updateSiteStatus } from '../../services/api';
import { Site, SiteStatus } from '../../types';
import { getApiError } from '../../utils/apiError';

export const SiteDetailPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasPermission } = usePermissions();
  const [site, setSite] = useState<Site | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(
    (location.state as { success?: string } | null)?.success || null
  );
  const [pendingDelete, setPendingDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const siteId = Number(id);

  const loadSite = async () => {
    setLoading(true);
    setError(null);
    try {
      setSite(await getSite(siteId));
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load site');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!Number.isFinite(siteId)) {
      setError('Invalid site id');
      setLoading(false);
      return;
    }
    loadSite();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [siteId]);

  const handleToggleStatus = async () => {
    if (!site) {
      return;
    }
    const nextStatus: SiteStatus = site.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      const updated = await updateSiteStatus(site.id, nextStatus);
      setSite(updated);
      setSuccess(`Site status updated to ${nextStatus}.`);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to update status');
    }
  };

  const handleDelete = async () => {
    if (!site) {
      return;
    }
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteSite(site.id);
      navigate('/sites', { state: { success: `Site ${site.siteCode} was deleted.` } });
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to delete site');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading site...</div>;
  }

  if (error && !site) {
    return <AlertBanner tone="error" message={error} />;
  }

  if (!site) {
    return <AlertBanner tone="error" message="Site not found." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wider text-slate-500">Site</p>
          <h1 className="text-2xl font-bold text-white">{site.siteName}</h1>
          <p className="text-sm text-slate-400">{site.siteCode}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/sites" className="px-3 py-2 text-sm rounded-lg border border-slate-700 hover:bg-slate-800">
            Back to list
          </Link>
          {hasPermission('UPDATE_SITE') && (
            <>
              <Link
                to={`/sites/${site.id}/edit`}
                className="px-3 py-2 text-sm rounded-lg bg-slate-800 border border-slate-700 hover:bg-slate-700"
              >
                Edit
              </Link>
              <button
                type="button"
                onClick={handleToggleStatus}
                className="px-3 py-2 text-sm rounded-lg border border-slate-700 hover:bg-slate-800"
              >
                {site.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
              </button>
            </>
          )}
          {hasPermission('DELETE_SITE') && (
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
        <Detail label="Status" value={<StatusBadge status={site.status} />} />
        <Detail
          label="Customer"
          value={
            hasPermission('VIEW_CUSTOMER') ? (
              <Link to={`/customers/${site.customerId}`} className="text-brand-300 hover:text-brand-200">
                {site.customerName} ({site.customerCode})
              </Link>
            ) : (
              `${site.customerName} (${site.customerCode})`
            )
          }
        />
        <Detail label="Contact name" value={site.contactName} />
        <Detail label="Contact phone" value={site.contactPhone} />
        <Detail label="Contact email" value={site.contactEmail} />
        <Detail label="City" value={site.city} />
        <Detail label="State" value={site.state} />
        <Detail label="Postal code" value={site.postalCode} />
        <Detail label="Country" value={site.country} />
        <Detail label="Address" value={[site.addressLine1, site.addressLine2].filter(Boolean).join(', ') || null} />
        <div className="md:col-span-2">
          <Detail label="Description" value={site.description} />
        </div>
        <Detail label="Created" value={new Date(site.createdAt).toLocaleString()} />
        <Detail label="Updated" value={new Date(site.updatedAt).toLocaleString()} />
      </div>

      <ConfirmDialog
        open={pendingDelete}
        title="Delete site"
        message={`Delete ${site.siteName}?`}
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
