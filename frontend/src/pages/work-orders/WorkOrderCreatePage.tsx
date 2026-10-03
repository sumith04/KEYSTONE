import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { createWorkOrder, getCustomers } from '../../services/api';
import { CreateWorkOrderRequest, Customer } from '../../types';
import { getApiError } from '../../utils/apiError';
import { WorkOrderForm } from './WorkOrderForm';

export const WorkOrderCreatePage: React.FC = () => {
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
        setError(getApiError(err).message || 'Unable to load customers for work order creation');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const handleSubmit = async (values: CreateWorkOrderRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const created = await createWorkOrder(values);
      navigate(`/work-orders/${created.id}`, {
        state: { success: `Work order ${created.workOrderNumber} was created.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to create work order');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading work order form...</div>;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Create work order</h1>
        <p className="text-sm text-slate-400">Open a new field service work order for a customer site.</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <WorkOrderForm
          customers={customers}
          submitLabel="Create work order"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/work-orders')}
        />
      </div>
    </div>
  );
};
