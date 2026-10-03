import React, { useEffect, useMemo, useState } from 'react';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';
import {
  addWorkOrderPart,
  deleteWorkOrderPart,
  getParts,
  getWorkOrderParts,
  updateWorkOrderPart,
} from '../../services/api';
import { Part, WorkOrder, WorkOrderPart, WorkOrderStatus } from '../../types';
import { getApiError } from '../../utils/apiError';
import { formatDateTime } from './WorkOrderBadges';

const PARTS_MUTABLE_STATUSES: WorkOrderStatus[] = ['ASSIGNED', 'IN_PROGRESS', 'ON_HOLD'];

const formatMoney = (value: number | string) => {
  const amount = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(amount)
    ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
    : String(value);
};

interface WorkOrderPartsSectionProps {
  workOrder: WorkOrder;
}

export const WorkOrderPartsSection: React.FC<WorkOrderPartsSectionProps> = ({ workOrder }) => {
  const { role, user } = useAuth();
  const { hasPermission } = usePermissions();
  const [usages, setUsages] = useState<WorkOrderPart[]>([]);
  const [catalog, setCatalog] = useState<Part[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [partId, setPartId] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [submitting, setSubmitting] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editQuantity, setEditQuantity] = useState('1');
  const [pendingDelete, setPendingDelete] = useState<WorkOrderPart | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const assignedToCurrentUser =
    role !== 'TECHNICIAN' || (user != null && workOrder.assignedTechnicianId === user.id);
  const canView = hasPermission('VIEW_PART');
  const canUse =
    hasPermission('USE_PARTS') &&
    assignedToCurrentUser &&
    PARTS_MUTABLE_STATUSES.includes(workOrder.status);

  const selectedPart = useMemo(
    () => catalog.find((part) => String(part.id) === partId) || null,
    [catalog, partId]
  );

  const loadUsages = async () => {
    setLoading(true);
    setError(null);
    try {
      setUsages(await getWorkOrderParts(workOrder.id));
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load work order parts');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!canView) {
      setLoading(false);
      return;
    }
    loadUsages();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workOrder.id, canView]);

  useEffect(() => {
    if (!canUse) {
      return;
    }
    getParts({ page: 0, size: 100, status: 'ACTIVE', sort: 'name,asc' })
      .then((data) => setCatalog(data.content))
      .catch(() => undefined);
  }, [canUse]);

  if (!canView) {
    return null;
  }

  const handleAdd = async (event: React.FormEvent) => {
    event.preventDefault();
    const selectedPartId = Number(partId);
    const selectedQuantity = Number(quantity);
    if (!Number.isFinite(selectedPartId) || selectedPartId <= 0) {
      setError('Select a part.');
      return;
    }
    if (!Number.isFinite(selectedQuantity) || selectedQuantity <= 0) {
      setError('Quantity must be greater than 0.');
      return;
    }
    if (selectedPart && selectedQuantity > selectedPart.quantityInStock) {
      setError(`Only ${selectedPart.quantityInStock} units are available.`);
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await addWorkOrderPart(workOrder.id, { partId: selectedPartId, quantity: selectedQuantity });
      setSuccess('Part usage recorded.');
      setShowForm(false);
      setPartId('');
      setQuantity('1');
      await loadUsages();
      const refreshed = await getParts({ page: 0, size: 100, status: 'ACTIVE', sort: 'name,asc' });
      setCatalog(refreshed.content);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to add part');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdate = async (usage: WorkOrderPart) => {
    const nextQuantity = Number(editQuantity);
    if (!Number.isFinite(nextQuantity) || nextQuantity <= 0) {
      setError('Quantity must be greater than 0.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await updateWorkOrderPart(workOrder.id, usage.id, { quantity: nextQuantity });
      setSuccess('Part quantity updated.');
      setEditingId(null);
      await loadUsages();
    } catch (err) {
      setError(getApiError(err).message || 'Unable to update part quantity');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteWorkOrderPart(workOrder.id, pendingDelete.id);
      setSuccess('Part usage removed and stock restored.');
      setPendingDelete(null);
      await loadUsages();
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to remove part usage');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Parts used</h2>
        {canUse && (
          <button
            type="button"
            onClick={() => {
              setShowForm((open) => !open);
              setError(null);
            }}
            className="px-3 py-2 text-sm rounded-lg bg-brand-600 hover:bg-brand-500 text-white"
          >
            {showForm ? 'Cancel' : 'Add Part'}
          </button>
        )}
      </div>

      {success && <AlertBanner tone="success" message={success} />}
      {error && <AlertBanner tone="error" message={error} />}

      {showForm && canUse && (
        <form onSubmit={handleAdd} className="grid grid-cols-1 md:grid-cols-3 gap-3 border border-slate-800 rounded-lg p-4">
          <label className="space-y-1 text-sm md:col-span-2">
            <span className="text-slate-300">Part</span>
            <select
              value={partId}
              onChange={(event) => setPartId(event.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
            >
              <option value="">Select a part</option>
              {catalog.map((part) => (
                <option key={part.id} value={part.id}>
                  {part.partNumber} — {part.name} ({part.quantityInStock} in stock)
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-1 text-sm">
            <span className="text-slate-300">Quantity</span>
            <input
              type="number"
              min="1"
              step="1"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
            />
          </label>
          {selectedPart && (
            <div className="md:col-span-3 text-xs text-slate-400">
              Available stock: {selectedPart.quantityInStock} · Catalog unit cost: {formatMoney(selectedPart.unitCost)}
            </div>
          )}
          <div className="md:col-span-3">
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 text-sm rounded-lg bg-brand-600 hover:bg-brand-500 text-white disabled:opacity-50"
            >
              {submitting ? 'Saving...' : 'Record usage'}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="text-sm text-slate-400">Loading parts...</div>
      ) : usages.length === 0 ? (
        <div className="text-sm text-slate-400">No parts have been used on this work order.</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="text-slate-400">
              <tr>
                <th className="text-left py-2 pr-3 font-medium">Part number</th>
                <th className="text-left py-2 pr-3 font-medium">Part name</th>
                <th className="text-left py-2 pr-3 font-medium">Qty</th>
                <th className="text-left py-2 pr-3 font-medium">Unit cost</th>
                <th className="text-left py-2 pr-3 font-medium">Total</th>
                <th className="text-left py-2 pr-3 font-medium">Used by</th>
                <th className="text-left py-2 pr-3 font-medium">Used at</th>
                {canUse && <th className="text-right py-2 font-medium">Actions</th>}
              </tr>
            </thead>
            <tbody>
              {usages.map((usage) => (
                <tr key={usage.id} className="border-t border-slate-800">
                  <td className="py-2 pr-3">{usage.partNumber}</td>
                  <td className="py-2 pr-3">{usage.partName}</td>
                  <td className="py-2 pr-3">
                    {editingId === usage.id ? (
                      <input
                        type="number"
                        min="1"
                        value={editQuantity}
                        onChange={(event) => setEditQuantity(event.target.value)}
                        className="w-20 rounded-md border border-slate-700 bg-slate-950 px-2 py-1"
                      />
                    ) : (
                      usage.quantityUsed
                    )}
                  </td>
                  <td className="py-2 pr-3">{formatMoney(usage.unitCostAtUsage)}</td>
                  <td className="py-2 pr-3">{formatMoney(usage.totalCost)}</td>
                  <td className="py-2 pr-3">{usage.usedByName || '—'}</td>
                  <td className="py-2 pr-3">{formatDateTime(usage.usedAt)}</td>
                  {canUse && (
                    <td className="py-2">
                      <div className="flex justify-end gap-2">
                        {editingId === usage.id ? (
                          <>
                            <button
                              type="button"
                              disabled={submitting}
                              onClick={() => handleUpdate(usage)}
                              className="px-2 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                            >
                              Save
                            </button>
                            <button
                              type="button"
                              onClick={() => setEditingId(null)}
                              className="px-2 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                            >
                              Cancel
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              type="button"
                              onClick={() => {
                                setEditingId(usage.id);
                                setEditQuantity(String(usage.quantityUsed));
                              }}
                              className="px-2 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                            >
                              Edit qty
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                setDeleteError(null);
                                setPendingDelete(usage);
                              }}
                              className="px-2 py-1 rounded-md border border-rose-800 text-rose-300 hover:bg-rose-950/40"
                            >
                              Remove
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Remove part usage"
        message={
          pendingDelete
            ? `Remove ${pendingDelete.quantityUsed} × ${pendingDelete.partNumber}? Stock will be restored.`
            : ''
        }
        busy={deleting}
        error={deleteError}
        onCancel={() => setPendingDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
};
