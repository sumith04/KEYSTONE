import React, { useMemo, useState } from 'react';
import { SlaPolicyRequest, WorkOrderPriority } from '../../types';

interface SlaPolicyFormProps {
  initialValues?: Partial<SlaPolicyRequest>;
  submitLabel: string;
  submitting: boolean;
  onSubmit: (values: SlaPolicyRequest) => Promise<void>;
  onCancel: () => void;
}

const emptyValues: SlaPolicyRequest = {
  name: '',
  description: '',
  priority: 'MEDIUM',
  responseTimeMinutes: 60,
  resolutionTimeMinutes: 240,
  active: true,
};

export const SlaPolicyForm: React.FC<SlaPolicyFormProps> = ({
  initialValues,
  submitLabel,
  submitting,
  onSubmit,
  onCancel,
}) => {
  const startingValues = useMemo(
    () => ({
      ...emptyValues,
      ...initialValues,
      description: initialValues?.description ?? '',
      active: initialValues?.active ?? true,
    }),
    [initialValues]
  );

  const [values, setValues] = useState<SlaPolicyRequest>(startingValues);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});

  const updateField = <K extends keyof SlaPolicyRequest>(field: K, value: SlaPolicyRequest[K]) => {
    setValues((current) => ({ ...current, [field]: value }));
  };

  const validate = (): boolean => {
    const nextErrors: Record<string, string> = {};
    if (!values.name.trim()) {
      nextErrors.name = 'Name is required';
    }
    if (!values.responseTimeMinutes || values.responseTimeMinutes <= 0) {
      nextErrors.responseTimeMinutes = 'Response time must be greater than 0';
    }
    if (!values.resolutionTimeMinutes || values.resolutionTimeMinutes <= 0) {
      nextErrors.resolutionTimeMinutes = 'Resolution time must be greater than 0';
    }
    if (
      values.responseTimeMinutes > 0 &&
      values.resolutionTimeMinutes > 0 &&
      values.resolutionTimeMinutes < values.responseTimeMinutes
    ) {
      nextErrors.resolutionTimeMinutes = 'Resolution time must be at least the response time';
    }
    setClientErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!validate()) {
      return;
    }
    await onSubmit({
      name: values.name.trim(),
      description: values.description?.trim() || undefined,
      priority: values.priority,
      responseTimeMinutes: Number(values.responseTimeMinutes),
      resolutionTimeMinutes: Number(values.resolutionTimeMinutes),
      active: values.active,
    });
  };

  const fieldClass =
    'w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-brand-500';

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <label className="space-y-1 text-sm md:col-span-2">
          <span className="text-slate-300">Name *</span>
          <input
            className={fieldClass}
            value={values.name}
            onChange={(event) => updateField('name', event.target.value)}
          />
          {clientErrors.name && <span className="text-xs text-rose-400">{clientErrors.name}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Priority *</span>
          <select
            className={fieldClass}
            value={values.priority}
            onChange={(event) => updateField('priority', event.target.value as WorkOrderPriority)}
          >
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </select>
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Active</span>
          <select
            className={fieldClass}
            value={values.active ? 'true' : 'false'}
            onChange={(event) => updateField('active', event.target.value === 'true')}
          >
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Response time (minutes) *</span>
          <input
            className={fieldClass}
            type="number"
            min="1"
            value={values.responseTimeMinutes}
            onChange={(event) => updateField('responseTimeMinutes', Number(event.target.value))}
          />
          {clientErrors.responseTimeMinutes && (
            <span className="text-xs text-rose-400">{clientErrors.responseTimeMinutes}</span>
          )}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Resolution time (minutes) *</span>
          <input
            className={fieldClass}
            type="number"
            min="1"
            value={values.resolutionTimeMinutes}
            onChange={(event) => updateField('resolutionTimeMinutes', Number(event.target.value))}
          />
          {clientErrors.resolutionTimeMinutes && (
            <span className="text-xs text-rose-400">{clientErrors.resolutionTimeMinutes}</span>
          )}
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
