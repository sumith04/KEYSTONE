import React, { useMemo, useState } from 'react';
import { CreateSiteRequest, Customer } from '../../types';

interface SiteFormProps {
  initialValues?: Partial<CreateSiteRequest>;
  customers: Customer[];
  submitLabel: string;
  submitting: boolean;
  onSubmit: (values: CreateSiteRequest) => Promise<void>;
  onCancel: () => void;
}

const emptyValues = {
  siteCode: '',
  siteName: '',
  customerId: '',
  addressLine1: '',
  addressLine2: '',
  city: '',
  state: '',
  postalCode: '',
  country: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  description: '',
};

const isValidEmail = (value: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

export const SiteForm: React.FC<SiteFormProps> = ({
  initialValues,
  customers,
  submitLabel,
  submitting,
  onSubmit,
  onCancel,
}) => {
  const startingValues = useMemo(
    () => ({
      ...emptyValues,
      siteCode: initialValues?.siteCode || '',
      siteName: initialValues?.siteName || '',
      customerId: initialValues?.customerId != null ? String(initialValues.customerId) : '',
      addressLine1: initialValues?.addressLine1 || '',
      addressLine2: initialValues?.addressLine2 || '',
      city: initialValues?.city || '',
      state: initialValues?.state || '',
      postalCode: initialValues?.postalCode || '',
      country: initialValues?.country || '',
      contactName: initialValues?.contactName || '',
      contactPhone: initialValues?.contactPhone || '',
      contactEmail: initialValues?.contactEmail || '',
      description: initialValues?.description || '',
    }),
    [initialValues]
  );

  const [values, setValues] = useState(startingValues);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});

  const updateField = (field: keyof typeof emptyValues, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
  };

  const validate = (): boolean => {
    const nextErrors: Record<string, string> = {};
    if (!values.siteCode.trim()) {
      nextErrors.siteCode = 'Site code is required';
    }
    if (!values.siteName.trim()) {
      nextErrors.siteName = 'Site name is required';
    }
    if (!values.customerId) {
      nextErrors.customerId = 'Customer is required';
    }
    if (values.contactEmail && !isValidEmail(values.contactEmail)) {
      nextErrors.contactEmail = 'Please provide a valid contact email address';
    }
    setClientErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!validate()) {
      return;
    }

    const payload: CreateSiteRequest = {
      siteCode: values.siteCode.trim(),
      siteName: values.siteName.trim(),
      customerId: Number(values.customerId),
      addressLine1: values.addressLine1.trim() || undefined,
      addressLine2: values.addressLine2.trim() || undefined,
      city: values.city.trim() || undefined,
      state: values.state.trim() || undefined,
      postalCode: values.postalCode.trim() || undefined,
      country: values.country.trim() || undefined,
      contactName: values.contactName.trim() || undefined,
      contactPhone: values.contactPhone.trim() || undefined,
      contactEmail: values.contactEmail.trim() || undefined,
      description: values.description.trim() || undefined,
    };

    await onSubmit(payload);
  };

  const fieldClass =
    'w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-brand-500';

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Site code *</span>
          <input className={fieldClass} value={values.siteCode} onChange={(event) => updateField('siteCode', event.target.value)} />
          {clientErrors.siteCode && <span className="text-xs text-rose-400">{clientErrors.siteCode}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Site name *</span>
          <input className={fieldClass} value={values.siteName} onChange={(event) => updateField('siteName', event.target.value)} />
          {clientErrors.siteName && <span className="text-xs text-rose-400">{clientErrors.siteName}</span>}
        </label>
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Customer *</span>
          <select className={fieldClass} value={values.customerId} onChange={(event) => updateField('customerId', event.target.value)}>
            <option value="">Select a customer</option>
            {customers.map((customer) => (
              <option key={customer.id} value={customer.id}>
                {customer.companyName} ({customer.customerCode})
              </option>
            ))}
          </select>
          {clientErrors.customerId && <span className="text-xs text-rose-400">{clientErrors.customerId}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Contact name</span>
          <input className={fieldClass} value={values.contactName} onChange={(event) => updateField('contactName', event.target.value)} />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Contact phone</span>
          <input className={fieldClass} value={values.contactPhone} onChange={(event) => updateField('contactPhone', event.target.value)} />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Contact email</span>
          <input className={fieldClass} type="email" value={values.contactEmail} onChange={(event) => updateField('contactEmail', event.target.value)} />
          {clientErrors.contactEmail && <span className="text-xs text-rose-400">{clientErrors.contactEmail}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Country</span>
          <input className={fieldClass} value={values.country} onChange={(event) => updateField('country', event.target.value)} />
        </label>
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Address line 1</span>
          <input className={fieldClass} value={values.addressLine1} onChange={(event) => updateField('addressLine1', event.target.value)} />
        </label>
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Address line 2</span>
          <input className={fieldClass} value={values.addressLine2} onChange={(event) => updateField('addressLine2', event.target.value)} />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">City</span>
          <input className={fieldClass} value={values.city} onChange={(event) => updateField('city', event.target.value)} />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">State</span>
          <input className={fieldClass} value={values.state} onChange={(event) => updateField('state', event.target.value)} />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Postal code</span>
          <input className={fieldClass} value={values.postalCode} onChange={(event) => updateField('postalCode', event.target.value)} />
        </label>
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Description</span>
          <textarea
            className={`${fieldClass} min-h-[120px]`}
            value={values.description}
            onChange={(event) => updateField('description', event.target.value)}
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
