import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { getCustomers, getWorkOrder, updateWorkOrder } from '../../services/api';
import { CreateWorkOrderRequest, Customer, WorkOrder } from '../../types';
import { getApiError } from '../../utils/apiError';
import { WorkOrderForm } from './WorkOrderForm';

export const WorkOrderEditPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const workOrderId = Number(id);
  const [workOrder, setWorkOrder] = useState<WorkOrder | null>(null);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const [current, customerPage] = await Promise.all([
          getWorkOrder(workOrderId),
          getCustomers({ page: 0, size: 100, sort: 'companyName' }),
        ]);
        setWorkOrder(current);
        setCustomers(customerPage.content);
      } catch (err) {
        setError(getApiError(err).message || 'Unable to load work order');
      } finally {
        setLoading(false);
      }
    };

    if (Number.isFinite(workOrderId)) {
      load();
    } else {
      setError('Invalid work order id');
      setLoading(false);
    }
  }, [workOrderId]);

  const handleSubmit = async (values: CreateWorkOrderRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const updated = await updateWorkOrder(workOrderId, values);
      navigate(`/work-orders/${updated.id}`, {
        state: { success: `Work order ${updated.workOrderNumber} was updated.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to update work order');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading work order...</div>;
  }

  if (!workOrder) {
    return <AlertBanner tone="error" message={error || 'Work order not found.'} />;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Edit work order</h1>
        <p className="text-sm text-slate-400">{workOrder.workOrderNumber}</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <WorkOrderForm
          initialValues={{
            title: workOrder.title,
            description: workOrder.description ?? undefined,
            customerId: workOrder.customerId,
            siteId: workOrder.siteId,
            priority: workOrder.priority,
            workType: workOrder.workType,
            scheduledStart: workOrder.scheduledStart ?? undefined,
            scheduledEnd: workOrder.scheduledEnd ?? undefined,
            notes: workOrder.notes ?? undefined,
          }}
          customers={customers}
          submitLabel="Save changes"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate(`/work-orders/${workOrder.id}`)}
        />
      </div>
    </div>
  );
};
