import { Notification, NotificationType, RelatedEntityType, Role } from '../types';

export const notificationPath = (
  relatedEntityType?: RelatedEntityType | null,
  relatedEntityId?: number | null,
  role?: Role | null
): string | null => {
  if (relatedEntityType === 'WORK_ORDER' && relatedEntityId) {
    return role === 'CUSTOMER' ? `/customer/work-orders/${relatedEntityId}` : `/work-orders/${relatedEntityId}`;
  }
  if (relatedEntityType === 'SERVICE_REQUEST' && relatedEntityId) {
    return role === 'CUSTOMER' ? `/customer/requests/${relatedEntityId}` : `/service-requests/${relatedEntityId}`;
  }
  if (relatedEntityType === 'PART') {
    return role === 'CUSTOMER' ? null : '/parts';
  }
  return null;
};

export const formatRelativeTime = (value: string): string => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const diffMs = Date.now() - date.getTime();
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (diffMs < minute) {
    return 'Just now';
  }
  if (diffMs < hour) {
    const minutes = Math.floor(diffMs / minute);
    return `${minutes} minute${minutes === 1 ? '' : 's'} ago`;
  }
  if (diffMs < day) {
    const hours = Math.floor(diffMs / hour);
    return `${hours} hour${hours === 1 ? '' : 's'} ago`;
  }
  if (diffMs < 7 * day) {
    const days = Math.floor(diffMs / day);
    return `${days} day${days === 1 ? '' : 's'} ago`;
  }
  return date.toLocaleString();
};

export const mergeIncomingNotification = (
  current: Notification[],
  incoming: Notification
): Notification[] => {
  const without = current.filter((item) => item.id !== incoming.id);
  return [incoming, ...without].sort(
    (left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
  );
};

export const notificationTypeLabel = (type: NotificationType): string => {
  switch (type) {
    case 'WORK_ORDER_ASSIGNED':
      return 'Assignment';
    case 'WORK_ORDER_STATUS_CHANGED':
      return 'Status';
    case 'WORK_ORDER_COMPLETED':
      return 'Completed';
    case 'WORK_ORDER_CLOSED':
      return 'Closed';
    case 'WORK_ORDER_CANCELLED':
      return 'Cancelled';
    case 'SLA_AT_RISK':
      return 'SLA at risk';
    case 'SLA_BREACHED':
      return 'SLA breached';
    case 'PART_LOW_STOCK':
      return 'Low stock';
    case 'SERVICE_REQUEST':
    case 'SERVICE_REQUEST_SUBMITTED':
    case 'SERVICE_REQUEST_ACKNOWLEDGED':
    case 'SERVICE_REQUEST_CONVERTED':
    case 'SERVICE_REQUEST_REJECTED':
      return 'Service request';
    default:
      return 'General';
  }
};
