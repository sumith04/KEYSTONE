import React, { useMemo, useState } from 'react';
import { CreatePartRequest, PartStatus } from '../../types';

interface PartFormProps {
  initialValues?: Partial<CreatePartRequest>;
  submitLabel: string;
  submitting: boolean;
  onSubmit: (values: CreatePartRequest) => Promise<void>;
  onCancel: () => void;
}

const emptyValues: CreatePartRequest = {
  partNumber: '',
  name: '',
  description: '',
  category: '',
  unitOfMeasure: '',
  unitCost: 0,
  quantityInStock: 0,
  reorderLevel: 0,
  status: 'ACTIVE',
};

export const PartForm: React.FC<PartFormProps> = ({
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
      category: initialValues?.category ?? '',
      unitOfMeasure: initialValues?.unitOfMeasure ?? '',
      status: initialValues?.status ?? 'ACTIVE',
    }),
    [initialValues]
  );

  const [values, setValues] = useState<CreatePartRequest>(startingValues);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});

  const updateField = <K extends keyof CreatePartRequest>(field: K, value: CreatePartRequest[K]) => {
    setValues((current) => ({ ...current, [field]: value }));
  };

  const validate = (): boolean => {
    const nextErrors: Record<string, string> = {};
    if (!values.partNumber.trim()) {
      nextErrors.partNumber = 'Part number is required';
    }
    if (!values.name.trim()) {
      nextErrors.name = 'Name is required';
    }
    if (values.unitCost == null || Number(values.unitCost) < 0) {
      nextErrors.unitCost = 'Unit cost must be at least 0';
    }
    if (values.quantityInStock == null || Number(values.quantityInStock) < 0) {
      nextErrors.quantityInStock = 'Quantity in stock must be at least 0';
    }
    if (values.reorderLevel == null || Number(values.reorderLevel) < 0) {
      nextErrors.reorderLevel = 'Reorder level must be at least 0';
    }
    setClientErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!validate()) {
      return;
    }

    const payload: CreatePartRequest = {
      partNumber: values.partNumber.trim(),
      name: values.name.trim(),
      description: values.description?.trim() || undefined,
      category: values.category?.trim() || undefined,
      unitOfMeasure: values.unitOfMeasure?.trim() || undefined,
      unitCost: Number(values.unitCost),
      quantityInStock: Number(values.quantityInStock),
      reorderLevel: Number(values.reorderLevel),
      status: values.status,
    };

    await onSubmit(payload);
  };

  const fieldClass =
    'w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-brand-500';

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Part number *</span>
          <input
            className={fieldClass}
            value={values.partNumber}
            onChange={(event) => updateField('partNumber', event.target.value)}
          />
          {clientErrors.partNumber && <span className="text-xs text-rose-400">{clientErrors.partNumber}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Name *</span>
          <input
            className={fieldClass}
            value={values.name}
            onChange={(event) => updateField('name', event.target.value)}
          />
          {clientErrors.name && <span className="text-xs text-rose-400">{clientErrors.name}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Category</span>
          <input
            className={fieldClass}
            value={values.category}
            onChange={(event) => updateField('category', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Unit of measure</span>
          <input
            className={fieldClass}
            value={values.unitOfMeasure}
            onChange={(event) => updateField('unitOfMeasure', event.target.value)}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Unit cost *</span>
          <input
            className={fieldClass}
            type="number"
            min="0"
            step="0.01"
            value={values.unitCost}
            onChange={(event) => updateField('unitCost', Number(event.target.value))}
          />
          {clientErrors.unitCost && <span className="text-xs text-rose-400">{clientErrors.unitCost}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Quantity in stock *</span>
          <input
            className={fieldClass}
            type="number"
            min="0"
            step="1"
            value={values.quantityInStock}
            onChange={(event) => updateField('quantityInStock', Number(event.target.value))}
          />
          {clientErrors.quantityInStock && (
            <span className="text-xs text-rose-400">{clientErrors.quantityInStock}</span>
          )}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Reorder level *</span>
          <input
            className={fieldClass}
            type="number"
            min="0"
            step="1"
            value={values.reorderLevel}
            onChange={(event) => updateField('reorderLevel', Number(event.target.value))}
          />
          {clientErrors.reorderLevel && <span className="text-xs text-rose-400">{clientErrors.reorderLevel}</span>}
        </label>
        <label className="space-y-1 text-sm">
          <span className="text-slate-300">Status</span>
          <select
            className={fieldClass}
            value={values.status}
            onChange={(event) => updateField('status', event.target.value as PartStatus)}
          >
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
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
