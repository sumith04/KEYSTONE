import React, { useEffect, useState } from 'react';
import { AlertBanner } from '../../components/AlertBanner';
import {
  getInventoryReport,
  getSlaReport,
  getTechnicianPerformanceReport,
  getTimeReport,
  getWorkOrderReport,
} from '../../services/api';
import {
  InventoryReport,
  SlaReport,
  TechnicianPerformance,
  TimeReport,
  WorkOrderReport,
} from '../../types';
import { getApiError } from '../../utils/apiError';
import { downloadCsv } from '../../utils/csv';

type ReportTab = 'work-orders' | 'sla' | 'technicians' | 'inventory' | 'time';

const fieldClass =
  'w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100';

export const ReportsPage: React.FC = () => {
  const [tab, setTab] = useState<ReportTab>('work-orders');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [appliedFrom, setAppliedFrom] = useState('');
  const [appliedTo, setAppliedTo] = useState('');
  const [workOrderReport, setWorkOrderReport] = useState<WorkOrderReport | null>(null);
  const [slaReport, setSlaReport] = useState<SlaReport | null>(null);
  const [technicianReport, setTechnicianReport] = useState<TechnicianPerformance[]>([]);
  const [inventoryReport, setInventoryReport] = useState<InventoryReport | null>(null);
  const [timeReport, setTimeReport] = useState<TimeReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadReports = async (nextFrom = appliedFrom, nextTo = appliedTo) => {
    setLoading(true);
    setError(null);
    const params = {
      from: nextFrom || undefined,
      to: nextTo || undefined,
    };
    try {
      const [workOrders, sla, technicians, inventory, time] = await Promise.all([
        getWorkOrderReport(params),
        getSlaReport(params),
        getTechnicianPerformanceReport(params),
        getInventoryReport(),
        getTimeReport(params),
      ]);
      setWorkOrderReport(workOrders);
      setSlaReport(sla);
      setTechnicianReport(technicians);
      setInventoryReport(inventory);
      setTimeReport(time);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load reports');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const applyFilters = (event: React.FormEvent) => {
    event.preventDefault();
    setAppliedFrom(from);
    setAppliedTo(to);
    loadReports(from, to);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Reports</h1>
        <p className="text-sm text-slate-400">
          Descriptive operational reports. Work order and SLA date filters use createdAt. Time uses time-log start
          time. Inventory is a current snapshot.
        </p>
      </div>

      <form onSubmit={applyFilters} className="grid grid-cols-1 sm:grid-cols-3 gap-3 bg-slate-900 border border-slate-800 rounded-xl p-4">
        <input type="date" value={from} onChange={(event) => setFrom(event.target.value)} className={fieldClass} />
        <input type="date" value={to} onChange={(event) => setTo(event.target.value)} className={fieldClass} />
        <button type="submit" className="rounded-lg bg-brand-600 hover:bg-brand-500 px-4 py-2 text-sm text-white">
          Apply dates
        </button>
      </form>

      <div className="flex flex-wrap gap-2">
        {(
          [
            ['work-orders', 'Work orders'],
            ['sla', 'SLA'],
            ['technicians', 'Technician performance'],
            ['inventory', 'Inventory'],
            ['time', 'Time'],
          ] as Array<[ReportTab, string]>
        ).map(([id, label]) => (
          <button
            key={id}
            type="button"
            onClick={() => setTab(id)}
            className={`px-3 py-1.5 rounded-lg text-sm border ${
              tab === id
                ? 'bg-slate-800 text-white border-slate-600'
                : 'bg-slate-950 text-slate-400 border-slate-800 hover:bg-slate-800'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {error && <AlertBanner tone="error" message={error} />}
      {loading ? (
        <div className="text-sm text-slate-400">Loading reports...</div>
      ) : (
        <>
          {tab === 'work-orders' && workOrderReport && (
            <ReportSection
              title="Work orders"
              onExport={() =>
                downloadCsv('work-order-report.csv', [
                  {
                    total: workOrderReport.total,
                    created: workOrderReport.created,
                    completed: workOrderReport.completed,
                    closed: workOrderReport.closed,
                    cancelled: workOrderReport.cancelled,
                    averageCompletionMinutes: workOrderReport.averageCompletionMinutes,
                  },
                  ...workOrderReport.statusDistribution.map((row) => ({
                    total: row.status,
                    created: row.count,
                    completed: '',
                    closed: '',
                    cancelled: '',
                    averageCompletionMinutes: '',
                  })),
                ])
              }
            >
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3 text-sm">
                <Stat label="Total / created" value={workOrderReport.total} />
                <Stat label="Completed" value={workOrderReport.completed} />
                <Stat label="Closed" value={workOrderReport.closed} />
                <Stat label="Cancelled" value={workOrderReport.cancelled} />
                <Stat
                  label="Avg completion (min)"
                  value={workOrderReport.averageCompletionMinutes ?? '—'}
                />
              </div>
            </ReportSection>
          )}

          {tab === 'sla' && slaReport && (
            <ReportSection
              title="SLA"
              onExport={() =>
                downloadCsv('sla-report.csv', [
                  {
                    withSla: slaReport.totalWorkOrdersWithSla,
                    resolvedWithinSla: slaReport.resolvedWithinSla,
                    breached: slaReport.breached,
                    atRisk: slaReport.atRisk,
                    compliance: slaReport.slaCompliancePercentage,
                    responseBreaches: slaReport.responseSlaBreaches,
                    resolutionBreaches: slaReport.resolutionSlaBreaches,
                  },
                ])
              }
            >
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3 text-sm">
                <Stat label="With SLA" value={slaReport.totalWorkOrdersWithSla} />
                <Stat label="Resolved within SLA" value={slaReport.resolvedWithinSla} />
                <Stat label="Breached" value={slaReport.breached} />
                <Stat label="At risk" value={slaReport.atRisk} />
                <Stat
                  label="Compliance %"
                  value={slaReport.slaCompliancePercentage ?? '—'}
                />
                <Stat label="Response breaches" value={slaReport.responseSlaBreaches} />
                <Stat label="Resolution breaches" value={slaReport.resolutionSlaBreaches} />
              </div>
            </ReportSection>
          )}

          {tab === 'technicians' && (
            <ReportSection
              title="Technician performance"
              onExport={() =>
                downloadCsv(
                  'technician-performance.csv',
                  technicianReport.map((row) => ({
                    technician: row.technicianName,
                    assigned: row.assignedCount,
                    completed: row.completedCount,
                    closed: row.closedCount,
                    completedWithinSla: row.completedWithinSla,
                    slaBreaches: row.slaBreaches,
                    loggedMinutes: row.totalLoggedMinutes,
                  }))
                )
              }
            >
              <div className="overflow-x-auto">
                <table className="min-w-full text-sm">
                  <thead className="bg-slate-950/80 text-slate-400">
                    <tr>
                      <th className="text-left px-3 py-2 font-medium">Technician</th>
                      <th className="text-right px-3 py-2 font-medium">Assigned</th>
                      <th className="text-right px-3 py-2 font-medium">Completed</th>
                      <th className="text-right px-3 py-2 font-medium">Closed</th>
                      <th className="text-right px-3 py-2 font-medium">Within SLA</th>
                      <th className="text-right px-3 py-2 font-medium">SLA breaches</th>
                      <th className="text-right px-3 py-2 font-medium">Logged min</th>
                    </tr>
                  </thead>
                  <tbody>
                    {technicianReport.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="px-3 py-4 text-slate-400">
                          No technician activity in this range.
                        </td>
                      </tr>
                    ) : (
                      technicianReport.map((row) => (
                        <tr key={row.technicianId} className="border-t border-slate-800">
                          <td className="px-3 py-2 text-slate-100">{row.technicianName}</td>
                          <td className="px-3 py-2 text-right">{row.assignedCount}</td>
                          <td className="px-3 py-2 text-right">{row.completedCount}</td>
                          <td className="px-3 py-2 text-right">{row.closedCount}</td>
                          <td className="px-3 py-2 text-right">{row.completedWithinSla}</td>
                          <td className="px-3 py-2 text-right">{row.slaBreaches}</td>
                          <td className="px-3 py-2 text-right">{row.totalLoggedMinutes}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </ReportSection>
          )}

          {tab === 'inventory' && inventoryReport && (
            <ReportSection
              title="Inventory"
              onExport={() =>
                downloadCsv('inventory-report.csv', [
                  {
                    totalParts: inventoryReport.totalParts,
                    activeParts: inventoryReport.activeParts,
                    inactiveParts: inventoryReport.inactiveParts,
                    lowStock: inventoryReport.lowStock,
                    outOfStock: inventoryReport.outOfStock,
                    totalInventoryValue: inventoryReport.totalInventoryValue,
                  },
                ])
              }
            >
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3 text-sm">
                <Stat label="Total parts" value={inventoryReport.totalParts} />
                <Stat label="Active" value={inventoryReport.activeParts} />
                <Stat label="Inactive" value={inventoryReport.inactiveParts} />
                <Stat label="Low stock" value={inventoryReport.lowStock} />
                <Stat label="Out of stock" value={inventoryReport.outOfStock} />
                <Stat label="Inventory value" value={inventoryReport.totalInventoryValue} />
              </div>
            </ReportSection>
          )}

          {tab === 'time' && timeReport && (
            <ReportSection
              title="Time"
              onExport={() =>
                downloadCsv('time-report.csv', [
                  {
                    section: 'summary',
                    name: 'Total',
                    minutes: timeReport.totalLoggedMinutes,
                    hours: timeReport.totalLoggedHours,
                    logs: timeReport.logsCount,
                  },
                  ...timeReport.minutesByTechnician.map((row) => ({
                    section: 'technician',
                    name: row.technicianName,
                    minutes: row.totalLoggedMinutes,
                    hours: '',
                    logs: '',
                  })),
                  ...timeReport.minutesByWorkOrder.map((row) => ({
                    section: 'work-order',
                    name: row.workOrderNumber,
                    minutes: row.totalLoggedMinutes,
                    hours: '',
                    logs: '',
                  })),
                ])
              }
            >
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3 text-sm mb-4">
                <Stat label="Logged minutes" value={timeReport.totalLoggedMinutes} />
                <Stat label="Logged hours" value={timeReport.totalLoggedHours} />
                <Stat label="Log count" value={timeReport.logsCount} />
              </div>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                <SimpleTable
                  title="Minutes by technician"
                  headers={['Technician', 'Minutes']}
                  rows={timeReport.minutesByTechnician.map((row) => [row.technicianName, String(row.totalLoggedMinutes)])}
                />
                <SimpleTable
                  title="Minutes by work order"
                  headers={['Work order', 'Minutes']}
                  rows={timeReport.minutesByWorkOrder.map((row) => [row.workOrderNumber, String(row.totalLoggedMinutes)])}
                />
              </div>
            </ReportSection>
          )}
        </>
      )}
    </div>
  );
};

const ReportSection: React.FC<{
  title: string;
  onExport: () => void;
  children: React.ReactNode;
}> = ({ title, onExport, children }) => (
  <section className="bg-slate-900 border border-slate-800 rounded-xl p-4 sm:p-6 space-y-4">
    <div className="flex items-center justify-between gap-3">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">{title}</h2>
      <button
        type="button"
        onClick={onExport}
        className="px-3 py-1.5 text-xs rounded-lg border border-slate-700 hover:bg-slate-800"
      >
        Export CSV
      </button>
    </div>
    {children}
  </section>
);

const Stat: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div className="rounded-lg border border-slate-800 bg-slate-950 px-3 py-3">
    <p className="text-xs uppercase tracking-wider text-slate-500">{label}</p>
    <p className="mt-1 text-lg font-semibold text-white">{value}</p>
  </div>
);

const SimpleTable: React.FC<{ title: string; headers: string[]; rows: string[][] }> = ({ title, headers, rows }) => (
  <div>
    <h3 className="text-xs uppercase tracking-wider text-slate-500 mb-2">{title}</h3>
    <div className="overflow-x-auto">
      <table className="min-w-full text-sm">
        <thead className="bg-slate-950/80 text-slate-400">
          <tr>
            {headers.map((header) => (
              <th key={header} className="text-left px-3 py-2 font-medium">
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td colSpan={headers.length} className="px-3 py-3 text-slate-400">
                No rows.
              </td>
            </tr>
          ) : (
            rows.map((row, index) => (
              <tr key={`${row[0]}-${index}`} className="border-t border-slate-800">
                {row.map((cell, cellIndex) => (
                  <td key={`${cell}-${cellIndex}`} className="px-3 py-2 text-slate-200">
                    {cell}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </div>
);
