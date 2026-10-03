import React, { useMemo, useState } from 'react';
import { CreateServiceRequest, Site, WorkOrderPriority } from '../../types';
import { fromDateTimeLocal, toDateTimeLocal } from '../../components/ServiceRequestBadges';

interface CustomerRequestFormProps {
  initialValues?: Partial<CreateServiceRequest>;
  sites: Site[];
  submitLabel: string;
  submitting: boolean;
  onSubmit: (values: CreateServiceRequest) => Promise<void>;
  onCancel: () => void;
}

const emptyValues = {
  siteId: '',
  title: '',
  description: '',
  priority: '' as WorkOrderPriority | '',
  preferredDate: '',
  contactName: '',
  contactPhone: '',
};

export const CustomerRequestForm: React.FC<CustomerRequestFormProps> = ({
  initialValues,
  sites,
  submitLabel,
  submitting,
  onSubmit,
  onCancel,
}) => {
  const startingValues = useMemo(
    () => ({
      ...emptyValues,
      siteId: initialValues?.siteId != null ? String(initialValues.siteId) : '',
      title: initialValues?.title || '',
      description: initialValues?.description || '',
      priority: initialValues?.priority || '',
      preferredDate: toDateTimeLocal(initialValues?.preferredDate),
      contactName: initialValues?.contactName || '',
      contactPhone: initialValues?.contactPhone || '',
    }),
    [initialValues]
  );

  const [values, setValues] = useState(startingValues);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});

  const updateField = (field: keyof typeof values, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const nextErrors: Record<string, string> = {};
    if (!values.siteId) {
      nextErrors.siteId = 'Site is required';
    }
    if (!values.title.trim()) {
      nextErrors.title = 'Title is required';
    }
    if (!values.description.trim()) {
      nextErrors.description = 'Description is required';
    }
    if (!values.priority) {
      nextErrors.priority = 'Priority is required';
    }
    setClientErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    await onSubmit({
      siteId: Number(values.siteId),
      title: values.title.trim(),
      description: values.description.trim(),
      priority: values.priority as WorkOrderPriority,
      preferredDate: fromDateTimeLocal(values.preferredDate) || null,
      contactName: values.contactName.trim() || undefined,
      contactPhone: values.contactPhone.trim() || undefined,
    });
  };

  const fieldClass = 'w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm';

  return (
    <form onSubmit={handleSubmit} className="space-y-5 bg-slate-900 border border-slate-800 rounded-xl p-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <label className="space-y-1">
          <span className="text-xs text-slate-400">Site</span>
          <select className={fieldClass} value={values.siteId} onChange={(event) => updateField('siteId', event.target.value)}>
            <option value="">Select a site</option>
            {sites.map((site) => (
              <option key={site.id} value={site.id}>
                {site.siteName} ({site.siteCode})
              </option>
            ))}
          </select>
          {clientErrors.siteId && <span className="text-xs text-rose-400">{clientErrors.siteId}</span>}
        </label>
        <label className="space-y-1">
          <span className="text-xs text-slate-400">Priority</span>
          <select
            className={fieldClass}
            value={values.priority}
            onChange={(event) => updateField('priority', event.target.value)}
          >
            <option value="">Select priority</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </select>
          {clientErrors.priority && <span className="text-xs text-rose-400">{clientErrors.priority}</span>}
        </label>
      </div>

      <label className="block space-y-1">
        <span className="text-xs text-slate-400">Title</span>
        <input
          className={fieldClass}
          value={values.title}
          onChange={(event) => updateField('title', event.target.value)}
          maxLength={200}
        />
        {clientErrors.title && <span className="text-xs text-rose-400">{clientErrors.title}</span>}
      </label>

      <label className="block space-y-1">
        <span className="text-xs text-slate-400">Description</span>
        <textarea
          className={`${fieldClass} min-h-32`}
          value={values.description}
          onChange={(event) => updateField('description', event.target.value)}
          maxLength={4000}
        />
        {clientErrors.description && <span className="text-xs text-rose-400">{clientErrors.description}</span>}
      </label>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <label className="space-y-1">
          <span className="text-xs text-slate-400">Preferred date</span>
          <input
            type="datetime-local"
            className={fieldClass}
            value={values.preferredDate}
            onChange={(event) => updateField('preferredDate', event.target.value)}
          />
        </label>
        <label className="space-y-1">
          <span className="text-xs text-slate-400">Contact name</span>
          <input
            className={fieldClass}
            value={values.contactName}
            onChange={(event) => updateField('contactName', event.target.value)}
            maxLength={120}
          />
        </label>
        <label className="space-y-1">
          <span className="text-xs text-slate-400">Contact phone</span>
          <input
            className={fieldClass}
            value={values.contactPhone}
            onChange={(event) => updateField('contactPhone', event.target.value)}
            maxLength={40}
          />
        </label>
      </div>

      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 rounded-lg border border-slate-700 text-sm text-slate-200 hover:bg-slate-800"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={submitting}
          className="px-4 py-2 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm disabled:opacity-50"
        >
          {submitting ? 'Saving...' : submitLabel}
        </button>
      </div>
    </form>
  );
};
