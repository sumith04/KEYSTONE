import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { getCustomer, updateCustomer } from '../../services/api';
import { CreateCustomerRequest, Customer } from '../../types';
import { getApiError } from '../../utils/apiError';
import { CustomerForm } from './CustomerForm';

export const CustomerEditPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const customerId = Number(id);
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        setCustomer(await getCustomer(customerId));
      } catch (err) {
        setError(getApiError(err).message || 'Unable to load customer');
      } finally {
        setLoading(false);
      }
    };

    if (Number.isFinite(customerId)) {
      load();
    } else {
      setError('Invalid customer id');
      setLoading(false);
    }
  }, [customerId]);

  const handleSubmit = async (values: CreateCustomerRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const updated = await updateCustomer(customerId, values);
      navigate(`/customers/${updated.id}`, {
        state: { success: `Customer ${updated.customerCode} was updated.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to update customer');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading customer...</div>;
  }

  if (!customer) {
    return <AlertBanner tone="error" message={error || 'Customer not found.'} />;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Edit customer</h1>
        <p className="text-sm text-slate-400">{customer.customerCode}</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <CustomerForm
          initialValues={{
            customerCode: customer.customerCode,
            companyName: customer.companyName,
            contactFirstName: customer.contactFirstName ?? undefined,
            contactLastName: customer.contactLastName ?? undefined,
            email: customer.email ?? undefined,
            phone: customer.phone ?? undefined,
            alternatePhone: customer.alternatePhone ?? undefined,
            addressLine1: customer.addressLine1 ?? undefined,
            addressLine2: customer.addressLine2 ?? undefined,
            city: customer.city ?? undefined,
            state: customer.state ?? undefined,
            postalCode: customer.postalCode ?? undefined,
            country: customer.country ?? undefined,
            notes: customer.notes ?? undefined,
          }}
          submitLabel="Save changes"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate(`/customers/${customer.id}`)}
        />
      </div>
    </div>
  );
};
