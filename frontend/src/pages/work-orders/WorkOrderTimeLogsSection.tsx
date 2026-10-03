import React, { useEffect, useState } from 'react';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useAuth } from '../../hooks/useAuth';
import { usePermissions } from '../../hooks/usePermissions';
import { addTimeLog, deleteTimeLog, getTimeLogs, updateTimeLog } from '../../services/api';
import { TimeLog, WorkOrder, WorkOrderStatus } from '../../types';
import { getApiError } from '../../utils/apiError';
import { formatDateTime, fromDateTimeLocal, toDateTimeLocal } from './WorkOrderBadges';

const TIME_MUTABLE_STATUSES: WorkOrderStatus[] = [
  'NEW',
  'ASSIGNED',
  'IN_PROGRESS',
  'ON_HOLD',
  'COMPLETED',
];

const formatDuration = (minutes: number) => {
  const hours = Math.floor(minutes / 60);
  const remaining = minutes % 60;
  if (hours === 0) {
    return `${remaining} min`;
  }
  return `${hours}h ${remaining}m`;
};

interface WorkOrderTimeLogsSectionProps {
  workOrder: WorkOrder;
}

export const WorkOrderTimeLogsSection: React.FC<WorkOrderTimeLogsSectionProps> = ({ workOrder }) => {
  const { role, user } = useAuth();
  const { hasPermission } = usePermissions();
  const [logs, setLogs] = useState<TimeLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [pendingDelete, setPendingDelete] = useState<TimeLog | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const assignedToCurrentUser =
    role !== 'TECHNICIAN' || (user != null && workOrder.assignedTechnicianId === user.id);
  const canView = hasPermission('VIEW_TIME_LOGS');
  const canAdd =
    hasPermission('ADD_TIME_LOG') &&
    assignedToCurrentUser &&
    TIME_MUTABLE_STATUSES.includes(workOrder.status);

  const loadLogs = async () => {
    setLoading(true);
    setError(null);
    try {
      setLogs(await getTimeLogs(workOrder.id));
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load time logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!canView) {
      setLoading(false);
      return;
    }
    loadLogs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workOrder.id, canView]);

  if (!canView) {
    return null;
  }

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setStartTime('');
    setEndTime('');
    setNotes('');
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!startTime || !endTime) {
      setError('Start time and end time are required.');
      return;
    }
    const payload = {
      startTime: fromDateTimeLocal(startTime) || startTime,
      endTime: fromDateTimeLocal(endTime) || endTime,
      notes: notes.trim() || undefined,
    };
    if (new Date(payload.endTime).getTime() <= new Date(payload.startTime).getTime()) {
      setError('End time must be after start time.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      if (editingId != null) {
        await updateTimeLog(workOrder.id, editingId, payload);
        setSuccess('Time log updated.');
      } else {
        await addTimeLog(workOrder.id, payload);
        setSuccess('Time log recorded.');
      }
      resetForm();
      await loadLogs();
    } catch (err) {
      setError(getApiError(err).message || 'Unable to save time log');
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
      await deleteTimeLog(workOrder.id, pendingDelete.id);
      setSuccess('Time log deleted.');
      setPendingDelete(null);
      await loadLogs();
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to delete time log');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Time logs</h2>
        {canAdd && (
          <button
            type="button"
            onClick={() => {
              resetForm();
              setShowForm(true);
              setError(null);
            }}
            className="px-3 py-2 text-sm rounded-lg bg-brand-600 hover:bg-brand-500 text-white"
          >
            Add Time Log
          </button>
        )}
      </div>

      {success && <AlertBanner tone="success" message={success} />}
      {error && <AlertBanner tone="error" message={error} />}

      {showForm && canAdd && (
        <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-3 border border-slate-800 rounded-lg p-4">
          <label className="space-y-1 text-sm">
            <span className="text-slate-300">Start time *</span>
            <input
              type="datetime-local"
              value={startTime}
              onChange={(event) => setStartTime(event.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="text-slate-300">End time *</span>
            <input
              type="datetime-local"
              value={endTime}
              onChange={(event) => setEndTime(event.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
            />
          </label>
          <label className="space-y-1 text-sm md:col-span-2">
            <span className="text-slate-300">Notes</span>
            <textarea
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm min-h-[90px]"
            />
          </label>
          <div className="md:col-span-2 text-xs text-slate-500">
            Duration is calculated by the server from start and end time.
          </div>
          <div className="md:col-span-2 flex gap-2">
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 text-sm rounded-lg bg-brand-600 hover:bg-brand-500 text-white disabled:opacity-50"
            >
              {submitting ? 'Saving...' : editingId != null ? 'Save time log' : 'Record time'}
            </button>
            <button
              type="button"
              onClick={resetForm}
              className="px-4 py-2 text-sm rounded-lg border border-slate-700 hover:bg-slate-800"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="text-sm text-slate-400">Loading time logs...</div>
      ) : logs.length === 0 ? (
        <div className="text-sm text-slate-400">No time logs have been recorded on this work order.</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="text-slate-400">
              <tr>
                <th className="text-left py-2 pr-3 font-medium">Technician</th>
                <th className="text-left py-2 pr-3 font-medium">Start</th>
                <th className="text-left py-2 pr-3 font-medium">End</th>
                <th className="text-left py-2 pr-3 font-medium">Duration</th>
                <th className="text-left py-2 pr-3 font-medium">Notes</th>
                {canAdd && <th className="text-right py-2 font-medium">Actions</th>}
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} className="border-t border-slate-800">
                  <td className="py-2 pr-3">{log.technicianName}</td>
                  <td className="py-2 pr-3">{formatDateTime(log.startTime)}</td>
                  <td className="py-2 pr-3">{formatDateTime(log.endTime)}</td>
                  <td className="py-2 pr-3">{formatDuration(log.durationMinutes)}</td>
                  <td className="py-2 pr-3 text-slate-400">{log.notes || '—'}</td>
                  {canAdd && (
                    <td className="py-2">
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setEditingId(log.id);
                            setShowForm(true);
                            setStartTime(toDateTimeLocal(log.startTime));
                            setEndTime(toDateTimeLocal(log.endTime));
                            setNotes(log.notes || '');
                          }}
                          className="px-2 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setDeleteError(null);
                            setPendingDelete(log);
                          }}
                          className="px-2 py-1 rounded-md border border-rose-800 text-rose-300 hover:bg-rose-950/40"
                        >
                          Delete
                        </button>
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
        title="Delete time log"
        message={pendingDelete ? `Delete the ${formatDuration(pendingDelete.durationMinutes)} time log?` : ''}
        busy={deleting}
        error={deleteError}
        onCancel={() => setPendingDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
};
