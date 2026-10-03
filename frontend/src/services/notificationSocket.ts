import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AUTH_TOKEN_KEY } from './api';
import { Notification } from '../types';
import {
  NOTIFICATION_SUBSCRIBE_DESTINATION,
  isJwtExpired,
  parseNotificationPayload,
  resolveSockJsEndpoint,
} from '../utils/websocket';

interface NotificationSocketOptions {
  token: string;
  onNotification: (notification: Notification) => void;
  onConnected?: () => void;
  onDisconnected?: () => void;
}

const logSocket = (message: string) => {
  if (import.meta.env.DEV) {
    console.info(`[keystone-ws] ${message}`);
  }
};

export const createNotificationSocket = ({
  token,
  onNotification,
  onConnected,
  onDisconnected,
}: NotificationSocketOptions): Client => {
  const endpoint = resolveSockJsEndpoint();

  const client = new Client({
    webSocketFactory: () => new SockJS(endpoint) as WebSocket,
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => undefined,
    beforeConnect: () => {
      const currentToken = localStorage.getItem(AUTH_TOKEN_KEY) || token;
      if (!currentToken || isJwtExpired(currentToken)) {
        logSocket('authentication expired; stopping reconnect');
        client.deactivate();
        throw new Error('WebSocket authentication is no longer valid');
      }
      client.connectHeaders = {
        Authorization: `Bearer ${currentToken}`,
      };
    },
    onConnect: () => {
      logSocket('connected');
      client.subscribe(NOTIFICATION_SUBSCRIBE_DESTINATION, (message: IMessage) => {
        try {
          const payload = message.body ? JSON.parse(message.body) : null;
          const notification = parseNotificationPayload(payload);
          if (!notification) {
            logSocket('ignored malformed notification payload');
            return;
          }
          onNotification(notification);
        } catch {
          logSocket('ignored unreadable notification payload');
        }
      });
      logSocket('subscribed to user notification queue');
      onConnected?.();
    },
    onDisconnect: () => {
      logSocket('disconnected');
      onDisconnected?.();
    },
    onStompError: (frame) => {
      logSocket(`unexpected STOMP error: ${frame.headers.message || 'unknown'}`);
      const headerMessage = (frame.headers.message || '').toLowerCase();
      if (headerMessage.includes('authentication') || headerMessage.includes('unauthorized')) {
        client.deactivate();
      }
    },
    onWebSocketError: () => {
      logSocket('unexpected WebSocket error');
    },
  });

  return client;
};
