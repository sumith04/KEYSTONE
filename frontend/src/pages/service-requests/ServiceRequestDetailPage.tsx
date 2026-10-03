import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { ServiceRequestPriorityBadge, ServiceRequestStatusBadge, formatDateTime } from '../../components/ServiceRequestBadges';
import { usePermissions } from '../../hooks/usePermissions';
import {
  acknowledgeServiceRequest,
  convertServiceRequestToWorkOrder,
  getServiceRequest,
  rejectServiceRequest,
  reviewServiceRequest,
} from '../../services/api';
import { ServiceRequest } from '../../types';
import { getApiError } from '../../utils/apiError';

export const ServiceRequestDetailPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasPermission } = usePermissions();
  const requestId = Number(id);
  const [request, setRequest] = useState<ServiceRequest | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [pendingAction, setPendingAction] = useState<'acknowledge' | 'review' | 'reject' | 'convert' | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const canUpdate = hasPermission('UPDATE_SERVICE_REQUEST');
  const canConvert = hasPermission('CONVERT_SERVICE_REQUEST');

  useEffect(() => {
    if (!Number.isFinite(requestId)) {
      setError('Invalid request id');
      setLoading(false);
      return;
    }
    getServiceRequest(requestId)
      .then(setRequest)
      .catch((err) => setError(getApiError(err).message || 'Unable to load service request'))
      .finally(() => setLoading(false));
  }, [requestId]);

  const runAction = async () => {
    if (!request || !pendingAction) {
      return;
    }
    setBusy(true);
    setActionError(null);
    setError(null);
    try {
      const actions = {
        acknowledge: {
          run: () => acknowledgeServiceRequest(request.id),
          success: 'Request acknowledged.',
        },
        review: {
          run: () => reviewServiceRequest(request.id),
          success: 'Request marked in review.',
        },
        reject: {
          run: () => rejectServiceRequest(request.id),
          success: 'Request rejected.',
        },
        convert: {
          run: () => convertServiceRequestToWorkOrder(request.id),
          success: 'Request converted to a work order.',
        },
      };
      const selected = actions[pendingAction];
      const updated = await selected.run();
      setRequest(updated);
      setSuccess(selected.success);
      setPendingAction(null);
    } catch (err) {
      const message = getApiError(err).message || 'Unable to update service request';
      setError(message);
      setActionError(message);
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <p className="text-sm text-slate-400">Loading service request...</p>;
  }

  if (error && !request) {
    return <AlertBanner tone="error" message={error} />;
  }

  if (!request) {
    return <AlertBanner tone="error" message="Service request not found." />;
  }

  const canAcknowledge = canUpdate && request.status === 'SUBMITTED';
  const canReview = canUpdate && request.status === 'ACKNOWLEDGED';
  const canReject = canUpdate && (request.status === 'ACKNOWLEDGED' || request.status === 'IN_REVIEW');
  const canConvertNow =
    canConvert && (request.status === 'ACKNOWLEDGED' || request.status === 'IN_REVIEW') && !request.workOrderId;

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wider text-slate-500">Incoming request</p>
          <h1 className="text-2xl font-bold text-white">{request.title}</h1>
          <p className="text-sm text-slate-400">{request.requestNumber}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => navigate('/service-requests')}
            className="px-3 py-2 rounded-lg border border-slate-700 text-sm text-slate-200 hover:bg-slate-800"
          >
            Back to list
          </button>
          {canAcknowledge && (
            <button
              type="button"
              onClick={() => setPendingAction('acknowledge')}
              className="px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-sm"
            >
              Acknowledge
            </button>
          )}
          {canReview && (
            <button
              type="button"
              onClick={() => setPendingAction('review')}
              className="px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-sm"
            >
              Mark in review
            </button>
          )}
          {canReject && (
            <button
              type="button"
              onClick={() => setPendingAction('reject')}
              className="px-3 py-2 rounded-lg bg-rose-700 hover:bg-rose-600 text-white text-sm"
            >
              Reject
            </button>
          )}
          {canConvertNow && (
            <button
              type="button"
              onClick={() => setPendingAction('convert')}
              className="px-3 py-2 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm"
            >
              Convert to work order
            </button>
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
              <dt className="text-xs uppercase tracking-wide text-slate-500">Customer</dt>
              <dd className="text-slate-200">{request.customerName || '—'}</dd>
            </div>
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
              <dd className="text-slate-200">
                {request.contactName || '—'}
                {request.contactPhone ? ` · ${request.contactPhone}` : ''}
              </dd>
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
              <Link to={`/work-orders/${request.workOrderId}`} className="text-brand-300 hover:text-brand-200">
                {request.workOrderNumber}
              </Link>
            ) : (
              <p className="text-slate-400">Not converted</p>
            )}
          </div>
        </div>
      </section>

      <ConfirmDialog
        open={pendingAction !== null}
        title={
          pendingAction === 'convert'
            ? 'Convert to work order?'
            : pendingAction === 'reject'
              ? 'Reject this request?'
              : pendingAction === 'review'
                ? 'Mark request in review?'
                : 'Acknowledge this request?'
        }
        message={
          pendingAction === 'convert'
            ? 'A work order will be created from this request using the same customer, site, title, and priority.'
            : pendingAction === 'reject'
              ? 'The customer will be notified that this request was rejected.'
              : 'The customer will receive a status update.'
        }
        confirmLabel={pendingAction === 'convert' ? 'Convert' : pendingAction === 'reject' ? 'Reject' : 'Continue'}
        busy={busy}
        error={actionError}
        onCancel={() => {
          setPendingAction(null);
          setActionError(null);
        }}
        onConfirm={runAction}
      />
    </div>
  );
};
