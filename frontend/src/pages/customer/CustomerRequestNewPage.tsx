import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { createMyServiceRequest, getCustomerSites } from '../../services/api';
import { CreateServiceRequest, Site } from '../../types';
import { getApiError } from '../../utils/apiError';
import { CustomerRequestForm } from './CustomerRequestForm';

export const CustomerRequestNewPage: React.FC = () => {
  const navigate = useNavigate();
  const [sites, setSites] = useState<Site[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  useEffect(() => {
    getCustomerSites({ page: 0, size: 100 })
      .then((data) => setSites(data.content))
      .catch((err) => setError(getApiError(err).message || 'Unable to load sites'))
      .finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (values: CreateServiceRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const created = await createMyServiceRequest(values);
      navigate(`/customer/requests/${created.id}`, {
        state: { success: `Request ${created.requestNumber} was submitted.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to create the service request');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">New service request</h1>
        <p className="text-sm text-slate-400">Describe the issue at one of your sites. Status is assigned by KEYSTONE.</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      {loading ? (
        <p className="text-sm text-slate-400">Loading sites...</p>
      ) : (
        <CustomerRequestForm
          sites={sites}
          submitLabel="Submit request"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/customer/requests')}
        />
      )}
    </div>
  );
};
