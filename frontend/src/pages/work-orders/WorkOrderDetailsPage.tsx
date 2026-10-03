import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { usePermissions } from '../../hooks/usePermissions';
import {
  assignWorkOrder,
  cancelWorkOrder,
  closeWorkOrder,
  completeWorkOrder,
  deleteWorkOrder,
  getAssignableTechnicians,
  holdWorkOrder,
  resumeWorkOrder,
  startWorkOrder,
  getWorkOrder,
} from '../../services/api';
import { AuthUser, WorkOrder, WorkOrderStatus } from '../../types';
import { getApiError } from '../../utils/apiError';
import { formatDateTime, formatWorkType, WorkOrderPriorityBadge, WorkOrderStatusBadge } from './WorkOrderBadges';

const editableStatuses: WorkOrderStatus[] = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD'];
const deletableStatuses: WorkOrderStatus[] = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'CANCELLED'];
const assignableStatuses: WorkOrderStatus[] = ['NEW', 'ASSIGNED'];

export const WorkOrderDetailsPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasPermission } = usePermissions();
  const [workOrder, setWorkOrder] = useState<WorkOrder | null>(null);
  const [technicians, setTechnicians] = useState<AuthUser[]>([]);
  const [selectedTechnicianId, setSelectedTechnicianId] = useState('');
  const [loading, setLoading] = useState(true);
  const [actionBusy, setActionBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(
    (location.state as { success?: string } | null)?.success || null
  );
  const [pendingDelete, setPendingDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const workOrderId = Number(id);

  const loadWorkOrder = async () => {
    setLoading(true);
    setError(null);
    try {
      setWorkOrder(await getWorkOrder(workOrderId));
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load work order');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!Number.isFinite(workOrderId)) {
      setError('Invalid work order id');
      setLoading(false);
      return;
    }
    loadWorkOrder();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workOrderId]);

  useEffect(() => {
    if (!hasPermission('ASSIGN_WORK_ORDER')) {
      return;
    }
    getAssignableTechnicians()
      .then(setTechnicians)
      .catch(() => undefined);
  }, [hasPermission]);

  useEffect(() => {
    if (workOrder?.assignedTechnicianId) {
      setSelectedTechnicianId(String(workOrder.assignedTechnicianId));
    }
  }, [workOrder?.assignedTechnicianId]);

  const runAction = async (action: () => Promise<WorkOrder>, successMessage: string) => {
    setActionBusy(true);
    setError(null);
    try {
      const updated = await action();
      setWorkOrder(updated);
      setSuccess(successMessage);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to update work order');
    } finally {
      setActionBusy(false);
    }
  };

  const handleAssign = async () => {
    if (!workOrder || !selectedTechnicianId) {
      setError('Select a technician before assigning this work order.');
      return;
    }
    await runAction(
      () => assignWorkOrder(workOrder.id, { technicianId: Number(selectedTechnicianId) }),
      `Work order ${workOrder.workOrderNumber} was assigned.`
    );
  };

  const handleDelete = async () => {
    if (!workOrder) {
      return;
    }
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteWorkOrder(workOrder.id);
      navigate('/work-orders', { state: { success: `Work order ${workOrder.workOrderNumber} was deleted.` } });
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to delete work order');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading work order...</div>;
  }

  if (error && !workOrder) {
    return <AlertBanner tone="error" message={error} />;
  }

  if (!workOrder) {
    return <AlertBanner tone="error" message="Work order not found." />;
  }

  const canAssign = hasPermission('ASSIGN_WORK_ORDER') && assignableStatuses.includes(workOrder.status);
  const canStart = hasPermission('START_WORK') && workOrder.status === 'ASSIGNED';
  const canHold = hasPermission('HOLD_WORK') && workOrder.status === 'IN_PROGRESS';
  const canResume = hasPermission('RESUME_WORK') && workOrder.status === 'ON_HOLD';
  const canComplete = hasPermission('COMPLETE_WORK') && workOrder.status === 'IN_PROGRESS';
  const canClose = hasPermission('CLOSE_WORK_ORDER') && workOrder.status === 'COMPLETED';
  const canCancel =
    hasPermission('CANCEL_WORK_ORDER') && ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD'].includes(workOrder.status);
  const canEdit = hasPermission('UPDATE_WORK_ORDER') && editableStatuses.includes(workOrder.status);
  const canDelete = hasPermission('DELETE_WORK_ORDER') && deletableStatuses.includes(workOrder.status);

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wider text-slate-500">Work Order</p>
          <h1 className="text-2xl font-bold text-white">{workOrder.workOrderNumber}</h1>
          <p className="text-sm text-slate-400">{workOrder.title}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link to="/work-orders" className="px-3 py-2 text-sm rounded-lg border border-slate-700 hover:bg-slate-800">
            Back to list
          </Link>
          {canEdit && (
            <Link
              to={`/work-orders/${workOrder.id}/edit`}
              className="px-3 py-2 text-sm rounded-lg bg-slate-800 border border-slate-700 hover:bg-slate-700"
            >
              Edit
            </Link>
          )}
          {canDelete && (
            <button
              type="button"
              onClick={() => setPendingDelete(true)}
              className="px-3 py-2 text-sm rounded-lg border border-rose-800 text-rose-300 hover:bg-rose-950/40"
            >
              Delete
            </button>
          )}
        </div>
      </div>

      {success && <AlertBanner tone="success" message={success} />}
      {error && <AlertBanner tone="error" message={error} />}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-900 border border-slate-800 rounded-xl p-6 text-sm">
        <Detail label="Status" value={<WorkOrderStatusBadge status={workOrder.status} />} />
        <Detail label="Priority" value={<WorkOrderPriorityBadge priority={workOrder.priority} />} />
        <Detail label="Work type" value={formatWorkType(workOrder.workType)} />
        <Detail label="Assigned technician" value={workOrder.assignedTechnicianName} />
        <Detail label="Customer" value={`${workOrder.customerName} (${workOrder.customerCode})`} />
        <Detail label="Site" value={`${workOrder.siteName} (${workOrder.siteCode})`} />
        <Detail label="Scheduled start" value={formatDateTime(workOrder.scheduledStart)} />
        <Detail label="Scheduled end" value={formatDateTime(workOrder.scheduledEnd)} />
        <Detail label="Actual start" value={formatDateTime(workOrder.actualStart)} />
        <Detail label="Actual end" value={formatDateTime(workOrder.actualEnd)} />
        <div className="md:col-span-2">
          <Detail label="Description" value={workOrder.description} />
        </div>
        <div className="md:col-span-2">
          <Detail label="Notes" value={workOrder.notes} />
        </div>
        <Detail label="Created by" value={workOrder.createdByName} />
        <Detail label="Created" value={formatDateTime(workOrder.createdAt)} />
        <Detail label="Updated" value={formatDateTime(workOrder.updatedAt)} />
      </div>

      {(canAssign || canStart || canHold || canResume || canComplete || canClose || canCancel) && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Lifecycle actions</h2>
          {canAssign && (
            <div className="flex flex-col sm:flex-row gap-3">
              <select
                value={selectedTechnicianId}
                onChange={(event) => setSelectedTechnicianId(event.target.value)}
                className="flex-1 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
              >
                <option value="">Select a technician</option>
                {technicians.map((technician) => (
                  <option key={technician.id} value={technician.id}>
                    {technician.firstName} {technician.lastName} ({technician.userEmail})
                  </option>
                ))}
              </select>
              <button
                type="button"
                disabled={actionBusy}
                onClick={handleAssign}
                className="px-4 py-2 text-sm rounded-lg bg-brand-600 hover:bg-brand-500 text-white disabled:opacity-50"
              >
                Assign
              </button>
            </div>
          )}
          <div className="flex flex-wrap gap-2">
            {canStart && (
              <ActionButton
                label="Start"
                busy={actionBusy}
                onClick={() => runAction(() => startWorkOrder(workOrder.id), 'Work started.')}
              />
            )}
            {canHold && (
              <ActionButton
                label="Hold"
                busy={actionBusy}
                onClick={() => runAction(() => holdWorkOrder(workOrder.id), 'Work placed on hold.')}
              />
            )}
            {canResume && (
              <ActionButton
                label="Resume"
                busy={actionBusy}
                onClick={() => runAction(() => resumeWorkOrder(workOrder.id), 'Work resumed.')}
              />
            )}
            {canComplete && (
              <ActionButton
                label="Complete"
                busy={actionBusy}
                onClick={() => runAction(() => completeWorkOrder(workOrder.id), 'Work completed.')}
              />
            )}
            {canClose && (
              <ActionButton
                label="Close"
                busy={actionBusy}
                onClick={() => runAction(() => closeWorkOrder(workOrder.id), 'Work order closed.')}
              />
            )}
            {canCancel && (
              <ActionButton
                label="Cancel"
                busy={actionBusy}
                danger
                onClick={() => runAction(() => cancelWorkOrder(workOrder.id), 'Work order cancelled.')}
              />
            )}
          </div>
        </div>
      )}

      <ConfirmDialog
        open={pendingDelete}
        title="Delete work order"
        message={`Delete ${workOrder.workOrderNumber}? Completed and closed work orders cannot be deleted.`}
        busy={deleting}
        error={deleteError}
        onCancel={() => setPendingDelete(false)}
        onConfirm={handleDelete}
      />
    </div>
  );
};

const Detail: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div>
    <p className="text-xs uppercase tracking-wider text-slate-500 mb-1">{label}</p>
    <div className="text-slate-200">{value || '—'}</div>
  </div>
);

const ActionButton: React.FC<{
  label: string;
  busy: boolean;
  danger?: boolean;
  onClick: () => void;
}> = ({ label, busy, danger, onClick }) => (
  <button
    type="button"
    disabled={busy}
    onClick={onClick}
    className={`px-3 py-2 text-sm rounded-lg border disabled:opacity-50 ${
      danger
        ? 'border-rose-800 text-rose-300 hover:bg-rose-950/40'
        : 'border-slate-700 hover:bg-slate-800'
    }`}
  >
    {label}
  </button>
);
