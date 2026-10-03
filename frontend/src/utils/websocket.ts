const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const NOTIFICATION_TYPES = new Set([
  'WORK_ORDER_ASSIGNED',
  'WORK_ORDER_STATUS_CHANGED',
  'WORK_ORDER_COMPLETED',
  'WORK_ORDER_CLOSED',
  'WORK_ORDER_CANCELLED',
  'SLA_AT_RISK',
  'SLA_BREACHED',
  'SERVICE_REQUEST',
  'PART_LOW_STOCK',
  'GENERAL',
]);

const RELATED_ENTITY_TYPES = new Set(['WORK_ORDER', 'PART']);

export const NOTIFICATION_SUBSCRIBE_DESTINATION = '/user/queue/notifications';

export const resolveSockJsEndpoint = (apiBaseUrl: string = API_BASE_URL): string => {
  const trimmed = apiBaseUrl.replace(/\/+$/, '');
  const origin = trimmed.endsWith('/api') ? trimmed.slice(0, -4) : trimmed;
  return `${origin}/ws`;
};

export const resolveNativeWebSocketUrl = (sockJsEndpoint: string = resolveSockJsEndpoint()): string => {
  if (sockJsEndpoint.startsWith('https://')) {
    return `wss://${sockJsEndpoint.slice('https://'.length)}`;
  }
  if (sockJsEndpoint.startsWith('http://')) {
    return `ws://${sockJsEndpoint.slice('http://'.length)}`;
  }
  if (sockJsEndpoint.startsWith('wss://') || sockJsEndpoint.startsWith('ws://')) {
    return sockJsEndpoint;
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}${sockJsEndpoint.startsWith('/') ? '' : '/'}${sockJsEndpoint}`;
};

export const isJwtExpired = (token: string): boolean => {
  try {
    const payloadPart = token.split('.')[1];
    if (!payloadPart) {
      return true;
    }
    const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(normalized)) as { exp?: number };
    return typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
};

const asNumber = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string' && value.trim() !== '' && Number.isFinite(Number(value))) {
    return Number(value);
  }
  return null;
};

const asString = (value: unknown): string | null => {
  return typeof value === 'string' ? value : null;
};

export const parseNotificationPayload = (body: unknown): import('../types').Notification | null => {
  if (!body || typeof body !== 'object') {
    return null;
  }
  const raw = body as Record<string, unknown>;
  const id = asNumber(raw.id);
  const type = asString(raw.type);
  const title = asString(raw.title);
  const message = asString(raw.message);
  const createdAt = asString(raw.createdAt);
  if (id == null || !type || !NOTIFICATION_TYPES.has(type) || !title || !message || !createdAt) {
    return null;
  }

  const relatedEntityType = asString(raw.relatedEntityType);
  if (relatedEntityType && !RELATED_ENTITY_TYPES.has(relatedEntityType)) {
    return null;
  }

  return {
    id,
    type: type as import('../types').NotificationType,
    title,
    message,
    relatedEntityType: (relatedEntityType as import('../types').RelatedEntityType | null) ?? null,
    relatedEntityId: raw.relatedEntityId == null ? null : asNumber(raw.relatedEntityId),
    read: Boolean(raw.read),
    createdAt,
    readAt: asString(raw.readAt),
  };
};
