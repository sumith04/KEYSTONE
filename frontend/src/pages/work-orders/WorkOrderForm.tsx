import React, { useEffect, useMemo, useState } from 'react';
import { getSites } from '../../services/api';
import { CreateWorkOrderRequest, Customer, Site, WorkOrderPriority, WorkType } from '../../types';
import { fromDateTimeLocal, toDateTimeLocal } from './WorkOrderBadges';

interface WorkOrderFormProps {
  initialValues?: Partial<CreateWorkOrderRequest>;
  customers: Customer[];
  submitLabel: string;
  submitting: boolean;
  onSubmit: (values: CreateWorkOrderRequest) => Promise<void>;
  onCancel: () => void;
}

const emptyValues = {
  title: '',
  description: '',
  customerId: '',
  siteId: '',
  priority: '' as WorkOrderPriority | '',
  workType: '' as WorkType | '',
  scheduledStart: '',
  scheduledEnd: '',
  notes: '',
};

export const WorkOrderForm: React.FC<WorkOrderFormProps> = ({
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
      title: initialValues?.title || '',
      description: initialValues?.description || '',
      customerId: initialValues?.customerId != null ? String(initialValues.customerId) : '',
      siteId: initialValues?.siteId != null ? String(initialValues.siteId) : '',
      priority: initialValues?.priority || '',
      workType: initialValues?.workType || '',
      scheduledStart: toDateTimeLocal(initialValues?.scheduledStart),
      scheduledEnd: toDateTimeLocal(initialValues?.scheduledEnd),
      notes: initialValues?.notes || '',
    }),
    [initialValues]
  );

  const [values, setValues] = useState(startingValues);
  const [sites, setSites] = useState<Site[]>([]);
  const [loadingSites, setLoadingSites] = useState(false);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!values.customerId) {
      setSites([]);
      return;
    }

    let cancelled = false;
    setLoadingSites(true);
    getSites({ page: 0, size: 100, customerId: Number(values.customerId) })
      .then((data) => {
        if (!cancelled) {
          setSites(data.content);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSites([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoadingSites(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [values.customerId]);

  const updateField = (field: keyof typeof emptyValues, value: string) => {
    setValues((current) => {
      if (field === 'customerId') {
        return { ...current, customerId: value, siteId: '' };
      }
      return { ...current, [field]: value };
    });
  };

  const validate = (): boolean => {
    const nextErrors: Record<string, string> = {};
    if (!values.title.trim()) {
      nextErrors.title = 'Title is required';
    }
    if (!values.customerId) {
      nextErrors.customerId = 'Customer is required';
    }
    if (!values.siteId) {
      nextErrors.siteId = 'Site is required';
    }
    if (!values.priority) {
      nextErrors.priority = 'Priority is required';
    }
    if (!values.workType) {
      nextErrors.workType = 'Work type is required';
    }
    if (values.scheduledStart && values.scheduledEnd && values.scheduledEnd < values.scheduledStart) {
      nextErrors.scheduledEnd = 'Scheduled end must not be before scheduled start';
    }

    const selectedSite = sites.find((site) => String(site.id) === values.siteId);
    if (values.customerId && values.siteId && selectedSite && String(selectedSite.customerId) !== values.customerId) {
      nextErrors.siteId = 'The selected site does not belong to the selected customer';
    }

    setClientErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!validate()) {
      return;
    }

    const payload: CreateWorkOrderRequest = {
      title: values.title.trim(),
      description: values.description.trim() || undefined,
      customerId: Number(values.customerId),
      siteId: Number(values.siteId),
      priority: values.priority as WorkOrderPriority,
      workType: values.workType as WorkType,
      scheduledStart: fromDateTimeLocal(values.scheduledStart),
      scheduledEnd: fromDateTimeLocal(values.scheduledEnd),
      notes: values.notes.trim() || undefined,
    };

    await onSubmit(payload);
  };

  const fieldClass =
    'w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-brand-500';

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Title *</span>
          <input className={fieldClass} value={values.title} onChange={(event) => updateField('title', event.target.value)} />
          {clientErrors.title && <span className="text-xs text-rose-400">{clientErrors.title}</span>}
        </label>
        <label className="space-y-1 text-sm">
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
          <span className="text-slate-300">Site *</span>
          <select
            className={fieldClass}
            value={values.siteId}
            onChange={(event) => updateField('siteId', event.target.value)}
            disabled={!values.customerId || loadingSites}
          >
            <option value="">{loadingSites ? 'Loading sites...' : values.customerId ? 'Select a site' : 'Select a customer first'}</option>
            {sites.map((site) => (
              <option key={site.id} value={site.id}>
                {site.siteName} ({site.siteCode})
              </option>
            ))}
          </select>
          {clientErrors.siteId && <span className="text-xs text-rose-400">{clientErrors.siteId}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Priority *</span>
          <select className={fieldClass} value={values.priority} onChange={(event) => updateField('priority', event.target.value)}>
            <option value="">Select priority</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </select>
          {clientErrors.priority && <span className="text-xs text-rose-400">{clientErrors.priority}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Work type *</span>
          <select className={fieldClass} value={values.workType} onChange={(event) => updateField('workType', event.target.value)}>
            <option value="">Select work type</option>
            <option value="PREVENTIVE_MAINTENANCE">Preventive maintenance</option>
            <option value="CORRECTIVE_MAINTENANCE">Corrective maintenance</option>
            <option value="INSPECTION">Inspection</option>
            <option value="EMERGENCY">Emergency</option>
            <option value="INSTALLATION">Installation</option>
            <option value="OTHER">Other</option>
          </select>
          {clientErrors.workType && <span className="text-xs text-rose-400">{clientErrors.workType}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Scheduled start</span>
          <input
            className={fieldClass}
            type="datetime-local"
            value={values.scheduledStart}
            onChange={(event) => updateField('scheduledStart', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Scheduled end</span>
          <input
            className={fieldClass}
            type="datetime-local"
            value={values.scheduledEnd}
            onChange={(event) => updateField('scheduledEnd', event.target.value)}
          />
          {clientErrors.scheduledEnd && <span className="text-xs text-rose-400">{clientErrors.scheduledEnd}</span>}
        </label>
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Description</span>
          <textarea
            className={`${fieldClass} min-h-[100px]`}
            value={values.description}
            onChange={(event) => updateField('description', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Notes</span>
          <textarea
            className={`${fieldClass} min-h-[100px]`}
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
