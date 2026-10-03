import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { createSite, getCustomers } from '../../services/api';
import { CreateSiteRequest, Customer } from '../../types';
import { getApiError } from '../../utils/apiError';
import { SiteForm } from './SiteForm';

export const SiteCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getCustomers({ page: 0, size: 100, sort: 'companyName' });
        setCustomers(data.content);
      } catch (err) {
        setError(getApiError(err).message || 'Unable to load customers for site assignment');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleSubmit = async (values: CreateSiteRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const created = await createSite(values);
      navigate(`/sites/${created.id}`, {
        state: { success: `Site ${created.siteCode} was created.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to create site');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading site form...</div>;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Create site</h1>
        <p className="text-sm text-slate-400">Add a facility and assign it to a customer.</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <SiteForm
          customers={customers}
          submitLabel="Create site"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/sites')}
        />
      </div>
    </div>
  );
};
