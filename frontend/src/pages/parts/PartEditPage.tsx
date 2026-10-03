import React, { useEffect, useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { useAuth } from '../../hooks/useAuth';
import { getPart, updatePart } from '../../services/api';
import { CreatePartRequest, Part, UpdatePartRequest } from '../../types';
import { getApiError } from '../../utils/apiError';
import { PartForm } from './PartForm';

export const PartEditPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { role } = useAuth();
  const partId = Number(id);
  const [part, setPart] = useState<Part | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        setPart(await getPart(partId));
      } catch (err) {
        setError(getApiError(err).message || 'Unable to load part');
      } finally {
        setLoading(false);
      }
    };

    if (Number.isFinite(partId)) {
      load();
    } else {
      setError('Invalid part id');
      setLoading(false);
    }
  }, [partId]);

  if (role === 'TECHNICIAN') {
    return <Navigate to="/parts" replace />;
  }

  const handleSubmit = async (values: CreatePartRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const payload: UpdatePartRequest = {
        ...values,
        status: values.status ?? 'ACTIVE',
      };
      const updated = await updatePart(partId, payload);
      navigate('/parts', {
        state: { success: `Part ${updated.partNumber} was updated.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to update part');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading part...</div>;
  }

  if (!part) {
    return <AlertBanner tone="error" message={error || 'Part not found.'} />;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Edit part</h1>
        <p className="text-sm text-slate-400">{part.partNumber}</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <PartForm
          initialValues={{
            partNumber: part.partNumber,
            name: part.name,
            description: part.description ?? undefined,
            category: part.category ?? undefined,
            unitOfMeasure: part.unitOfMeasure ?? undefined,
            unitCost: Number(part.unitCost),
            quantityInStock: part.quantityInStock,
            reorderLevel: part.reorderLevel,
            status: part.status,
          }}
          submitLabel="Save changes"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/parts')}
        />
      </div>
    </div>
  );
};
