import React, { useEffect, useMemo, useState } from 'react';
import { usePermissions } from '../../hooks/usePermissions';
import { getSlaPolicies } from '../../services/api';
import { CreateCustomerRequest, SlaPolicy } from '../../types';

interface CustomerFormProps {
  initialValues?: Partial<CreateCustomerRequest>;
  submitLabel: string;
  submitting: boolean;
  onSubmit: (values: CreateCustomerRequest) => Promise<void>;
  onCancel: () => void;
}

const emptyValues: CreateCustomerRequest = {
  customerCode: '',
  companyName: '',
  contactFirstName: '',
  contactLastName: '',
  email: '',
  phone: '',
  alternatePhone: '',
  addressLine1: '',
  addressLine2: '',
  city: '',
  state: '',
  postalCode: '',
  country: '',
  notes: '',
  slaPolicyId: undefined,
};

const isValidEmail = (value: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

export const CustomerForm: React.FC<CustomerFormProps> = ({
  initialValues,
  submitLabel,
  submitting,
  onSubmit,
  onCancel,
}) => {
  const startingValues = useMemo(
    () => ({
      ...emptyValues,
      ...Object.fromEntries(
        Object.entries(initialValues || {}).map(([key, value]) => [key, value ?? ''])
      ),
    }),
    [initialValues]
  );

  const { hasPermission } = usePermissions();
  const [values, setValues] = useState<CreateCustomerRequest>(startingValues);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});
  const [policies, setPolicies] = useState<SlaPolicy[]>([]);
  const canViewSla = hasPermission('VIEW_SLA');

  useEffect(() => {
    if (!canViewSla) {
      return;
    }
    getSlaPolicies({ page: 0, size: 100, sort: 'name,asc' })
      .then((data) => setPolicies(data.content))
      .catch(() => undefined);
  }, [canViewSla]);

  const updateField = <K extends keyof CreateCustomerRequest>(field: K, value: CreateCustomerRequest[K]) => {
    setValues((current) => ({ ...current, [field]: value }));
  };

  const validate = (): boolean => {
    const nextErrors: Record<string, string> = {};
    if (!values.customerCode.trim()) {
      nextErrors.customerCode = 'Customer code is required';
    }
    if (!values.companyName.trim()) {
      nextErrors.companyName = 'Company name is required';
    }
    if (values.email && !isValidEmail(values.email)) {
      nextErrors.email = 'Please provide a valid email address';
    }
    setClientErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!validate()) {
      return;
    }

    const payload: CreateCustomerRequest = {
      customerCode: values.customerCode.trim(),
      companyName: values.companyName.trim(),
      contactFirstName: values.contactFirstName?.trim() || undefined,
      contactLastName: values.contactLastName?.trim() || undefined,
      email: values.email?.trim() || undefined,
      phone: values.phone?.trim() || undefined,
      alternatePhone: values.alternatePhone?.trim() || undefined,
      addressLine1: values.addressLine1?.trim() || undefined,
      addressLine2: values.addressLine2?.trim() || undefined,
      city: values.city?.trim() || undefined,
      state: values.state?.trim() || undefined,
      postalCode: values.postalCode?.trim() || undefined,
      country: values.country?.trim() || undefined,
      notes: values.notes?.trim() || undefined,
      slaPolicyId: values.slaPolicyId || undefined,
    };

    await onSubmit(payload);
  };

  const fieldClass =
    'w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-brand-500';

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Customer code *</span>
          <input
            className={fieldClass}
            value={values.customerCode}
            onChange={(event) => updateField('customerCode', event.target.value)}
          />
          {clientErrors.customerCode && <span className="text-xs text-rose-400">{clientErrors.customerCode}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Company name *</span>
          <input
            className={fieldClass}
            value={values.companyName}
            onChange={(event) => updateField('companyName', event.target.value)}
          />
          {clientErrors.companyName && <span className="text-xs text-rose-400">{clientErrors.companyName}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Contact first name</span>
          <input
            className={fieldClass}
            value={values.contactFirstName}
            onChange={(event) => updateField('contactFirstName', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Contact last name</span>
          <input
            className={fieldClass}
            value={values.contactLastName}
            onChange={(event) => updateField('contactLastName', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Email</span>
          <input
            className={fieldClass}
            type="email"
            value={values.email}
            onChange={(event) => updateField('email', event.target.value)}
          />
          {clientErrors.email && <span className="text-xs text-rose-400">{clientErrors.email}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Phone</span>
          <input
            className={fieldClass}
            value={values.phone}
            onChange={(event) => updateField('phone', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Alternate phone</span>
          <input
            className={fieldClass}
            value={values.alternatePhone}
            onChange={(event) => updateField('alternatePhone', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Country</span>
          <input
            className={fieldClass}
            value={values.country}
            onChange={(event) => updateField('country', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Address line 1</span>
          <input
            className={fieldClass}
            value={values.addressLine1}
            onChange={(event) => updateField('addressLine1', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Address line 2</span>
          <input
            className={fieldClass}
            value={values.addressLine2}
            onChange={(event) => updateField('addressLine2', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">City</span>
          <input
            className={fieldClass}
            value={values.city}
            onChange={(event) => updateField('city', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">State</span>
          <input
            className={fieldClass}
            value={values.state}
            onChange={(event) => updateField('state', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Postal code</span>
          <input
            className={fieldClass}
            value={values.postalCode}
            onChange={(event) => updateField('postalCode', event.target.value)}
          />
        </label>
        {canViewSla && (
          <label className="space-y-1 text-sm md:col-span-2">
            <span className="text-slate-300">SLA policy</span>
            <select
              className={fieldClass}
              value={values.slaPolicyId ?? ''}
              onChange={(event) =>
                updateField('slaPolicyId', event.target.value ? Number(event.target.value) : undefined)
              }
            >
              <option value="">No SLA</option>
              {policies.map((policy) => (
                <option key={policy.id} value={policy.id}>
                  {policy.name} ({policy.active ? 'Active' : 'Inactive'})
                </option>
              ))}
            </select>
          </label>
        )}
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Notes</span>
          <textarea
            className={`${fieldClass} min-h-[120px]`}
            value={values.notes}
            onChange={(event) => updateField('notes', event.target.value)}
          />
        </label>
      </div>

      <div className="flex justify-end space-x-3">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-sm rounded-lg border border-slate-700 text-slate-200 hover:bg-slate-800"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={submitting}
          className="px-4 py-2 text-sm rounded-lg bg-brand-600 hover:bg-brand-500 text-white disabled:opacity-50"
        >
          {submitting ? 'Saving...' : submitLabel}
        </button>
      </div>
    </form>
  );
};
