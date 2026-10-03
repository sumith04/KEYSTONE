import type { LucideIcon } from 'lucide-react';
import {
  BarChart3,
  Bell,
  Building2,
  ClipboardList,
  Inbox,
  LayoutDashboard,
  Package,
  Timer,
  Users,
  Wrench,
} from 'lucide-react';
import { Role } from '../types';

export interface ModuleNavItem {
  to: string;
  label: string;
  description: string;
  icon: LucideIcon;
  permission?: string;
  visibleForRoles?: Role[];
  hiddenForRoles?: Role[];
}

export const internalModuleNav: ModuleNavItem[] = [
  {
    to: '/dashboard',
    label: 'Dashboard',
    description: 'Operational summary, SLA warnings, and recent work.',
    icon: LayoutDashboard,
    permission: 'VIEW_DASHBOARD',
  },
  {
    to: '/customers',
    label: 'Customers',
    description: 'Manage customer accounts and contacts.',
    icon: Users,
    permission: 'VIEW_CUSTOMER',
  },
  {
    to: '/sites',
    label: 'Sites',
    description: 'Facilities and site contacts linked to customers.',
    icon: Building2,
    permission: 'VIEW_SITE',
  },
  {
    to: '/work-orders',
    label: 'Work Orders',
    description: 'Create, assign, and track the work-order lifecycle.',
    icon: ClipboardList,
    permission: 'VIEW_WORK_ORDER',
    hiddenForRoles: ['TECHNICIAN'],
  },
  {
    to: '/technician',
    label: 'Technician Workspace',
    description: 'Assigned jobs, status updates, parts, and time logs.',
    icon: Wrench,
    permission: 'VIEW_WORK_ORDER',
    visibleForRoles: ['TECHNICIAN'],
  },
  {
    to: '/parts',
    label: 'Parts',
    description: 'Inventory, stock levels, and part records.',
    icon: Package,
    permission: 'VIEW_PART',
  },
  {
    to: '/sla-policies',
    label: 'SLA Policies',
    description: 'Response and resolution policy configuration.',
    icon: Timer,
    permission: 'VIEW_SLA',
  },
  {
    to: '/reports',
    label: 'Reports',
    description: 'Work order, SLA, technician, and inventory reports.',
    icon: BarChart3,
    permission: 'VIEW_REPORT',
  },
  {
    to: '/service-requests',
    label: 'Service Requests',
    description: 'Review incoming customer requests and convert them.',
    icon: Inbox,
    permission: 'VIEW_SERVICE_REQUEST',
  },
  {
    to: '/notifications',
    label: 'Notifications',
    description: 'Inbox for assignments, status changes, and SLA alerts.',
    icon: Bell,
  },
];

export const isModuleVisible = (
  item: ModuleNavItem,
  role: Role | null,
  hasPermission: (permission: string) => boolean
): boolean => {
  if (item.hiddenForRoles && role && item.hiddenForRoles.includes(role)) {
    return false;
  }
  if (item.visibleForRoles && (!role || !item.visibleForRoles.includes(role))) {
    return false;
  }
  if (item.permission && !hasPermission(item.permission)) {
    return false;
  }
  return true;
};
