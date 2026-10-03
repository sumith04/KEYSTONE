import React, { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from '../services/api';
import { Notification } from '../types';
import { mergeIncomingNotification } from '../utils/notifications';

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
    setRecentNotifications((current) => mergeIncomingNotification(current, notification).slice(0, 8));
    if (!notification.read) {
      setUnreadCount((count) => count + 1);
    }
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      setRecentNotifications([]);
      return;
    }
    refreshUnreadCount().catch(() => {
      setUnreadCount(0);
    });
  }, [isAuthenticated, refreshUnreadCount]);

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
