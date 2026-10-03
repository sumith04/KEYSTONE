import React, { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuth } from '../hooks/useAuth';
import {
  AUTH_TOKEN_KEY,
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from '../services/api';
import { createNotificationSocket } from '../services/notificationSocket';
import { Notification } from '../types';
import { mergeIncomingNotification } from '../utils/notifications';
import { isJwtExpired } from '../utils/websocket';

export interface NotificationContextValue {
  unreadCount: number;
  recentNotifications: Notification[];
  recentLoading: boolean;
  refreshUnreadCount: () => Promise<void>;
  loadRecentNotifications: () => Promise<void>;
  markRead: (id: number) => Promise<Notification | null>;
  markAllRead: () => Promise<void>;
  applyIncomingNotification: (notification: Notification) => void;
}

export const NotificationContext = createContext<NotificationContextValue | undefined>(undefined);

interface NotificationProviderProps {
  children: React.ReactNode;
}

export const NotificationProvider: React.FC<NotificationProviderProps> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [recentNotifications, setRecentNotifications] = useState<Notification[]>([]);
  const [recentLoading, setRecentLoading] = useState(false);
  const seenIdsRef = useRef<Set<number>>(new Set());
  const socketRef = useRef<Client | null>(null);

  const refreshUnreadCount = useCallback(async () => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      return;
    }
    const response = await getUnreadNotificationCount();
    setUnreadCount(response.unreadCount);
  }, [isAuthenticated]);

  const loadRecentNotifications = useCallback(async () => {
    if (!isAuthenticated) {
      setRecentNotifications([]);
      return;
    }
    setRecentLoading(true);
    try {
      const page = await getNotifications({ page: 0, size: 8 });
      page.content.forEach((item) => seenIdsRef.current.add(item.id));
      setRecentNotifications(page.content);
      setUnreadCount(page.unreadCount);
    } finally {
      setRecentLoading(false);
    }
  }, [isAuthenticated]);

  const markRead = useCallback(async (id: number) => {
    const updated = await markNotificationAsRead(id);
    setRecentNotifications((current) =>
      current.map((item) => (item.id === id ? updated : item))
    );
    const response = await getUnreadNotificationCount();
    setUnreadCount(response.unreadCount);
    return updated;
  }, []);

  const markAllRead = useCallback(async () => {
    const response = await markAllNotificationsAsRead();
    setUnreadCount(response.unreadCount);
    setRecentNotifications((current) =>
      current.map((item) =>
        item.read ? item : { ...item, read: true, readAt: item.readAt || new Date().toISOString() }
      )
    );
  }, []);

  const applyIncomingNotification = useCallback((notification: Notification) => {
    if (!notification || typeof notification.id !== 'number') {
      return;
    }
    const isNew = !seenIdsRef.current.has(notification.id);
    seenIdsRef.current.add(notification.id);
    setRecentNotifications((current) => mergeIncomingNotification(current, notification).slice(0, 8));
    if (isNew && !notification.read) {
      setUnreadCount((count) => count + 1);
    }
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      setRecentNotifications([]);
      seenIdsRef.current = new Set();
      return;
    }
    refreshUnreadCount().catch(() => {
      setUnreadCount(0);
    });
  }, [isAuthenticated, refreshUnreadCount]);

  useEffect(() => {
    if (!isAuthenticated) {
      socketRef.current?.deactivate();
      socketRef.current = null;
      return;
    }

    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    if (!token || isJwtExpired(token)) {
      return;
    }

    const client = createNotificationSocket({
      token,
      onNotification: applyIncomingNotification,
      onConnected: () => {
        refreshUnreadCount().catch(() => undefined);
        loadRecentNotifications().catch(() => undefined);
      },
    });
    socketRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
      if (socketRef.current === client) {
        socketRef.current = null;
      }
    };
  }, [isAuthenticated, applyIncomingNotification, refreshUnreadCount, loadRecentNotifications]);

  const value = useMemo<NotificationContextValue>(
    () => ({
      unreadCount,
      recentNotifications,
      recentLoading,
      refreshUnreadCount,
      loadRecentNotifications,
      markRead,
      markAllRead,
      applyIncomingNotification,
    }),
    [
      unreadCount,
      recentNotifications,
      recentLoading,
      refreshUnreadCount,
      loadRecentNotifications,
      markRead,
      markAllRead,
      applyIncomingNotification,
    ]
  );

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
};
