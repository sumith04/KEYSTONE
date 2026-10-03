import React, { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { useAuth } from '../../hooks/useAuth';
import { createPart } from '../../services/api';
import { CreatePartRequest } from '../../types';
import { getApiError } from '../../utils/apiError';
import { PartForm } from './PartForm';

export const PartCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const { role } = useAuth();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  if (role === 'TECHNICIAN') {
    return <Navigate to="/parts" replace />;
  }

  const handleSubmit = async (values: CreatePartRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const created = await createPart(values);
      navigate('/parts', {
        state: { success: `Part ${created.partNumber} was created.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to create part');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Create part</h1>
        <p className="text-sm text-slate-400">Add a catalog part and starting inventory.</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <PartForm
          submitLabel="Create part"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/parts')}
        />
      </div>
    </div>
  );
};
