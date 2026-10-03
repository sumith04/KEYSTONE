import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { createCustomer } from '../../services/api';
import { CreateCustomerRequest } from '../../types';
import { getApiError } from '../../utils/apiError';
import { CustomerForm } from './CustomerForm';

export const CustomerCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  const handleSubmit = async (values: CreateCustomerRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const created = await createCustomer(values);
      navigate(`/customers/${created.id}`, {
        state: { success: `Customer ${created.customerCode} was created.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to create customer');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Create customer</h1>
        <p className="text-sm text-slate-400">Add a commercial customer account.</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <CustomerForm
          submitLabel="Create customer"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/customers')}
        />
      </div>
    </div>
  );
};
