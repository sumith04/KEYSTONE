import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Search } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Pagination } from '../../components/Pagination';
import { usePermissions } from '../../hooks/usePermissions';
import { deleteWorkOrder, getAssignableTechnicians, getCustomers, getWorkOrders } from '../../services/api';
import { AuthUser, Customer, WorkOrder, WorkOrderPageResponse, WorkOrderPriority, WorkOrderStatus } from '../../types';
import { getApiError } from '../../utils/apiError';
import { formatDateTime, formatWorkType, WorkOrderPriorityBadge, WorkOrderStatusBadge } from './WorkOrderBadges';

const editableStatuses: WorkOrderStatus[] = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD'];
const deletableStatuses: WorkOrderStatus[] = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'CANCELLED'];

export const WorkOrdersPage: React.FC = () => {
  const navigate = useNavigate();
  const { hasPermission } = usePermissions();
  const [pageData, setPageData] = useState<WorkOrderPageResponse | null>(null);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [technicians, setTechnicians] = useState<AuthUser[]>([]);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<WorkOrderStatus | ''>('');
  const [priority, setPriority] = useState<WorkOrderPriority | ''>('');
  const [customerId, setCustomerId] = useState<number | ''>('');
  const [technicianId, setTechnicianId] = useState<number | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<WorkOrder | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const canCreate = hasPermission('CREATE_WORK_ORDER');
  const canUpdate = hasPermission('UPDATE_WORK_ORDER');
  const canDelete = hasPermission('DELETE_WORK_ORDER');
  const canViewCustomers = hasPermission('VIEW_CUSTOMER');
  const canFilterTechnicians = hasPermission('ASSIGN_WORK_ORDER') || hasPermission('VIEW_USER');

  const loadWorkOrders = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getWorkOrders({
        page,
        size: 10,
        search,
        status: status || undefined,
        priority: priority || undefined,
        customerId: customerId === '' ? undefined : customerId,
        technicianId: technicianId === '' ? undefined : technicianId,
        sort: 'createdAt,desc',
      });
      setPageData(data);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load work orders');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWorkOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, search, status, priority, customerId, technicianId]);

  useEffect(() => {
    if (canViewCustomers) {
      getCustomers({ page: 0, size: 100, sort: 'companyName' })
        .then((data) => setCustomers(data.content))
        .catch(() => undefined);
    }
    if (canFilterTechnicians) {
      getAssignableTechnicians()
        .then(setTechnicians)
        .catch(() => undefined);
    }
  }, [canViewCustomers, canFilterTechnicians]);

  const handleSearch = (event: React.FormEvent) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  const handleDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteWorkOrder(pendingDelete.id);
      setSuccess(`Work order ${pendingDelete.workOrderNumber} was deleted.`);
      setPendingDelete(null);
      await loadWorkOrders();
    } catch (err) {
      setDeleteError(getApiError(err).message || 'Unable to delete work order');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Work Orders</h1>
          <p className="text-sm text-slate-400">Create, assign, and manage field service work orders.</p>
        </div>
        {canCreate && (
          <Link
            to="/work-orders/new"
            className="inline-flex items-center justify-center space-x-2 px-4 py-2 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Create work order</span>
          </Link>
        )}
      </div>

      <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-6 gap-3 bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="md:col-span-2 relative">
          <Search className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search number, title, or description"
            className="w-full rounded-lg border border-slate-700 bg-slate-950 pl-9 pr-3 py-2 text-sm"
          />
        </div>
        <select
          value={status}
          onChange={(event) => {
            setPage(0);
            setStatus(event.target.value as WorkOrderStatus | '');
          }}
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
        >
          <option value="">All statuses</option>
          <option value="NEW">New</option>
          <option value="ASSIGNED">Assigned</option>
          <option value="IN_PROGRESS">In progress</option>
          <option value="ON_HOLD">On hold</option>
          <option value="COMPLETED">Completed</option>
          <option value="CLOSED">Closed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <select
          value={priority}
          onChange={(event) => {
            setPage(0);
            setPriority(event.target.value as WorkOrderPriority | '');
          }}
          className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
        >
          <option value="">All priorities</option>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
          <option value="URGENT">Urgent</option>
        </select>
        {canViewCustomers ? (
          <select
            value={customerId}
            onChange={(event) => {
              setPage(0);
              setCustomerId(event.target.value ? Number(event.target.value) : '');
            }}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
          >
            <option value="">All customers</option>
            {customers.map((customer) => (
              <option key={customer.id} value={customer.id}>
                {customer.companyName}
              </option>
            ))}
          </select>
        ) : (
          <div className="rounded-lg border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-500">
            Customer filter unavailable
          </div>
        )}
        {canFilterTechnicians ? (
          <select
            value={technicianId}
            onChange={(event) => {
              setPage(0);
              setTechnicianId(event.target.value ? Number(event.target.value) : '');
            }}
            className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm"
          >
            <option value="">All technicians</option>
            {technicians.map((technician) => (
              <option key={technician.id} value={technician.id}>
                {technician.firstName} {technician.lastName}
              </option>
            ))}
          </select>
        ) : (
          <button type="submit" className="rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-4 py-2 text-sm">
            Search
          </button>
        )}
        {canFilterTechnicians && (
          <button type="submit" className="md:col-span-6 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 px-4 py-2 text-sm">
            Search
          </button>
        )}
      </form>

      {success && <AlertBanner tone="success" message={success} />}
      {error && <AlertBanner tone="error" message={error} />}

      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-400">Loading work orders...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-6 text-sm text-slate-400">No work orders match the current filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/80 text-slate-400">
                <tr>
                  <th className="text-left px-4 py-3 font-medium">Number</th>
                  <th className="text-left px-4 py-3 font-medium">Title</th>
                  <th className="text-left px-4 py-3 font-medium">Customer</th>
                  <th className="text-left px-4 py-3 font-medium">Site</th>
                  <th className="text-left px-4 py-3 font-medium">Technician</th>
                  <th className="text-left px-4 py-3 font-medium">Status</th>
                  <th className="text-left px-4 py-3 font-medium">Priority</th>
                  <th className="text-left px-4 py-3 font-medium">Work type</th>
                  <th className="text-left px-4 py-3 font-medium">Scheduled</th>
                  <th className="text-right px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((workOrder) => (
                  <tr key={workOrder.id} className="border-t border-slate-800">
                    <td className="px-4 py-3 font-medium text-slate-100">{workOrder.workOrderNumber}</td>
                    <td className="px-4 py-3">{workOrder.title}</td>
                    <td className="px-4 py-3 text-slate-400">{workOrder.customerName}</td>
                    <td className="px-4 py-3 text-slate-400">{workOrder.siteName}</td>
                    <td className="px-4 py-3 text-slate-400">{workOrder.assignedTechnicianName || 'Unassigned'}</td>
                    <td className="px-4 py-3">
                      <WorkOrderStatusBadge status={workOrder.status} />
                    </td>
                    <td className="px-4 py-3">
                      <WorkOrderPriorityBadge priority={workOrder.priority} />
                    </td>
                    <td className="px-4 py-3 text-slate-400">{formatWorkType(workOrder.workType)}</td>
                    <td className="px-4 py-3 text-slate-400">{formatDateTime(workOrder.scheduledStart)}</td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => navigate(`/work-orders/${workOrder.id}`)}
                          className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                        >
                          View
                        </button>
                        {canUpdate && editableStatuses.includes(workOrder.status) && (
                          <button
                            type="button"
                            onClick={() => navigate(`/work-orders/${workOrder.id}/edit`)}
                            className="px-2.5 py-1 rounded-md border border-slate-700 hover:bg-slate-800"
                          >
                            Edit
                          </button>
                        )}
                        {canDelete && deletableStatuses.includes(workOrder.status) && (
                          <button
                            type="button"
                            onClick={() => {
                              setDeleteError(null);
                              setPendingDelete(workOrder);
                            }}
                            className="px-2.5 py-1 rounded-md border border-rose-800 text-rose-300 hover:bg-rose-950/50"
                          >
                            Delete
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {pageData && (
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onPageChange={setPage}
        />
      )}

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Delete work order"
        message={
          pendingDelete
            ? `Delete ${pendingDelete.workOrderNumber}? Completed and closed work orders cannot be deleted.`
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
