import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { createSlaPolicy } from '../../services/api';
import { SlaPolicyRequest } from '../../types';
import { getApiError } from '../../utils/apiError';
import { SlaPolicyForm } from './SlaPolicyForm';

export const SlaPolicyCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  const handleSubmit = async (values: SlaPolicyRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const created = await createSlaPolicy(values);
      navigate('/sla-policies', {
        state: { success: `SLA policy ${created.name} was created.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to create SLA policy');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Create SLA policy</h1>
        <p className="text-sm text-slate-400">Set response and resolution targets in minutes.</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <SlaPolicyForm
          submitLabel="Create policy"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/sla-policies')}
        />
      </div>
    </div>
  );
};
