import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { getSlaPolicy, updateSlaPolicy } from '../../services/api';
import { SlaPolicy, SlaPolicyRequest } from '../../types';
import { getApiError } from '../../utils/apiError';
import { SlaPolicyForm } from './SlaPolicyForm';

export const SlaPolicyEditPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const policyId = Number(id);
  const [policy, setPolicy] = useState<SlaPolicy | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        setPolicy(await getSlaPolicy(policyId));
      } catch (err) {
        setError(getApiError(err).message || 'Unable to load SLA policy');
      } finally {
        setLoading(false);
      }
    };

    if (Number.isFinite(policyId)) {
      load();
    } else {
      setError('Invalid SLA policy id');
      setLoading(false);
    }
  }, [policyId]);

  const handleSubmit = async (values: SlaPolicyRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const updated = await updateSlaPolicy(policyId, values);
      navigate('/sla-policies', {
        state: { success: `SLA policy ${updated.name} was updated.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to update SLA policy');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading SLA policy...</div>;
  }

  if (!policy) {
    return <AlertBanner tone="error" message={error || 'SLA policy not found.'} />;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Edit SLA policy</h1>
        <p className="text-sm text-slate-400">{policy.name}</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <SlaPolicyForm
          initialValues={{
            name: policy.name,
            description: policy.description ?? undefined,
            priority: policy.priority,
            responseTimeMinutes: policy.responseTimeMinutes,
            resolutionTimeMinutes: policy.resolutionTimeMinutes,
            active: policy.active,
          }}
          submitLabel="Save changes"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/sla-policies')}
        />
      </div>
    </div>
  );
};
