import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { getCustomerSites, getMyServiceRequest, updateMyServiceRequest } from '../../services/api';
import { CreateServiceRequest, ServiceRequest, Site } from '../../types';
import { getApiError } from '../../utils/apiError';
import { CustomerRequestForm } from './CustomerRequestForm';

export const CustomerRequestEditPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const requestId = Number(id);
  const [request, setRequest] = useState<ServiceRequest | null>(null);
  const [sites, setSites] = useState<Site[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  useEffect(() => {
    if (!Number.isFinite(requestId)) {
      setError('Invalid request id');
      setLoading(false);
      return;
    }
    Promise.all([getMyServiceRequest(requestId), getCustomerSites({ page: 0, size: 100 })])
      .then(([current, sitePage]) => {
        if (current.status !== 'SUBMITTED') {
          navigate(`/customer/requests/${current.id}`, { replace: true });
          return;
        }
        setRequest(current);
        setSites(sitePage.content);
      })
      .catch((err) => setError(getApiError(err).message || 'Unable to load request'))
      .finally(() => setLoading(false));
  }, [navigate, requestId]);

  const handleSubmit = async (values: CreateServiceRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const updated = await updateMyServiceRequest(requestId, values);
      navigate(`/customer/requests/${updated.id}`, {
        state: { success: `Request ${updated.requestNumber} was updated.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to update the service request');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
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

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Edit request {request.requestNumber}</h1>
        <p className="text-sm text-slate-400">Submitted requests can be updated until KEYSTONE starts processing them.</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <CustomerRequestForm
        initialValues={{
          siteId: request.siteId,
          title: request.title,
          description: request.description,
          priority: request.priority,
          preferredDate: request.preferredDate,
          contactName: request.contactName ?? undefined,
          contactPhone: request.contactPhone ?? undefined,
        }}
        sites={sites}
        submitLabel="Save changes"
        submitting={submitting}
        onSubmit={handleSubmit}
        onCancel={() => navigate(`/customer/requests/${request.id}`)}
      />
    </div>
  );
};
