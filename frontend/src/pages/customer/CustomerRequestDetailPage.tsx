import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { ServiceRequestPriorityBadge, ServiceRequestStatusBadge, formatDateTime } from '../../components/ServiceRequestBadges';
import { cancelMyServiceRequest, getMyServiceRequest } from '../../services/api';
import { ServiceRequest } from '../../types';
import { getApiError } from '../../utils/apiError';

export const CustomerRequestDetailPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const requestId = Number(id);
  const [request, setRequest] = useState<ServiceRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(
    (location.state as { success?: string } | null)?.success || null
  );
  const [pendingCancel, setPendingCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(requestId)) {
      setError('Invalid request id');
      setLoading(false);
      return;
    }
    setLoading(true);
    getMyServiceRequest(requestId)
      .then(setRequest)
      .catch((err) => setError(getApiError(err).message || 'Unable to load request'))
      .finally(() => setLoading(false));
  }, [requestId]);

  const handleCancel = async () => {
    if (!request) {
      return;
    }
    setCancelling(true);
    setCancelError(null);
    try {
      const updated = await cancelMyServiceRequest(request.id);
      setRequest(updated);
      setSuccess(`Request ${updated.requestNumber} was cancelled.`);
      setPendingCancel(false);
    } catch (err) {
      setCancelError(getApiError(err).message || 'Unable to cancel request');
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return <p className="text-sm text-slate-400">Loading request...</p>;
  }

  if (error && !request) {
    return <AlertBanner tone="error" message={error} />;
  }

  if (!request) {
    return <AlertBanner tone="error" message="Request not found." />;
  }

  const canEdit = request.status === 'SUBMITTED';

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wider text-slate-500">Service request</p>
          <h1 className="text-2xl font-bold text-white">{request.title}</h1>
          <p className="text-sm text-slate-400">{request.requestNumber}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => navigate('/customer/requests')}
            className="px-3 py-2 rounded-lg border border-slate-700 text-sm text-slate-200 hover:bg-slate-800"
          >
            Back to requests
          </button>
          {canEdit && (
            <>
              <Link
                to={`/customer/requests/${request.id}/edit`}
                className="px-3 py-2 rounded-lg border border-slate-700 text-sm text-slate-200 hover:bg-slate-800"
              >
                Edit
              </Link>
              <button
                type="button"
                onClick={() => setPendingCancel(true)}
                className="px-3 py-2 rounded-lg bg-rose-700 hover:bg-rose-600 text-white text-sm"
              >
                Cancel request
              </button>
            </>
          )}
        </div>
      </div>

      {success && <AlertBanner tone="success" message={success} />}
      {error && <AlertBanner tone="error" message={error} />}

      <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            <ServiceRequestStatusBadge status={request.status} />
            <ServiceRequestPriorityBadge priority={request.priority} />
          </div>
          <p className="text-sm text-slate-300 whitespace-pre-wrap">{request.description}</p>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Site</dt>
              <dd className="text-slate-200">{request.siteName || '—'} {request.siteCode ? `(${request.siteCode})` : ''}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Preferred date</dt>
              <dd className="text-slate-200">{formatDateTime(request.preferredDate)}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Contact</dt>
              <dd className="text-slate-200">{request.contactName || '—'}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-slate-500">Phone</dt>
              <dd className="text-slate-200">{request.contactPhone || '—'}</dd>
            </div>
          </dl>
        </div>
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4 text-sm">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Requested</p>
            <p className="text-slate-200">{formatDateTime(request.requestedAt)}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Updated</p>
            <p className="text-slate-200">{formatDateTime(request.updatedAt)}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Work order</p>
            {request.workOrderId ? (
              <Link to={`/customer/work-orders/${request.workOrderId}`} className="text-brand-300 hover:text-brand-200">
                {request.workOrderNumber}
              </Link>
            ) : (
              <p className="text-slate-400">Not converted yet</p>
            )}
          </div>
        </div>
      </section>

      <ConfirmDialog
        open={pendingCancel}
        title="Cancel this request?"
        message="Cancelled requests cannot be reopened. Raise a new request if the issue returns."
        confirmLabel="Cancel request"
        busy={cancelling}
        error={cancelError}
        onCancel={() => setPendingCancel(false)}
        onConfirm={handleCancel}
      />
    </div>
  );
};
