import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import { useNotifications } from '../hooks/useNotifications';
import { RelatedEntityType } from '../types';
import { formatRelativeTime, notificationPath, notificationTypeLabel } from '../utils/notifications';

export const NotificationBell: React.FC = () => {
  const navigate = useNavigate();
  const {
    unreadCount,
    recentNotifications,
    recentLoading,
    loadRecentNotifications,
    markRead,
    markAllRead,
  } = useNotifications();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (open) {
      loadRecentNotifications().catch(() => undefined);
    }
  }, [open, loadRecentNotifications]);

  useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const handleNotificationClick = async (
    id: number,
    relatedEntityType: RelatedEntityType | null,
    relatedEntityId: number | null
  ) => {
    try {
      await markRead(id);
    } catch {
      // Navigation should still proceed for a reachable related record.
    }
    const path = notificationPath(relatedEntityType, relatedEntityId);
    setOpen(false);
    if (path) {
      navigate(path);
    }
  };

  const handleMarkAll = async () => {
    try {
      await markAllRead();
    } catch {
      // Keep the dropdown open so the user can retry.
    }
  };

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="relative p-2 rounded-lg border border-slate-700 text-slate-300 hover:bg-slate-800"
        aria-label="Notifications"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-1.5 -right-1.5 min-w-[1.15rem] h-5 px-1 rounded-full bg-rose-500 text-white text-[10px] font-semibold flex items-center justify-center">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-[22rem] max-w-[calc(100vw-2rem)] rounded-xl border border-slate-800 bg-slate-900 shadow-xl z-50">
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-800">
            <div>
              <p className="text-sm font-semibold text-white">Notifications</p>
              <p className="text-xs text-slate-500">
                {unreadCount} unread
              </p>
            </div>
            <button
              type="button"
              onClick={handleMarkAll}
              className="inline-flex items-center space-x-1 text-xs text-slate-300 hover:text-white"
            >
              <CheckCheck className="w-3.5 h-3.5" />
              <span>Mark all as read</span>
            </button>
          </div>

          <div className="max-h-80 overflow-y-auto">
            {recentLoading && (
              <p className="px-4 py-6 text-sm text-slate-500">Loading notifications...</p>
            )}
            {!recentLoading && recentNotifications.length === 0 && (
              <p className="px-4 py-6 text-sm text-slate-500">No notifications yet.</p>
            )}
            {!recentLoading &&
              recentNotifications.map((notification) => (
                <button
                  key={notification.id}
                  type="button"
                  onClick={() =>
                    handleNotificationClick(
                      notification.id,
                      notification.relatedEntityType,
                      notification.relatedEntityId
                    )
                  }
                  className={`w-full text-left px-4 py-3 border-b border-slate-800/80 hover:bg-slate-800/60 ${
                    notification.read ? 'opacity-70' : ''
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-white truncate">{notification.title}</p>
                      <p className="text-xs text-slate-400 mt-1 line-clamp-2">{notification.message}</p>
                    </div>
                    {!notification.read && (
                      <span className="mt-1 w-2 h-2 rounded-full bg-brand-400 shrink-0" />
                    )}
                  </div>
                  <div className="mt-2 flex items-center justify-between text-[11px] text-slate-500">
                    <span>{notificationTypeLabel(notification.type)}</span>
                    <span>{formatRelativeTime(notification.createdAt)}</span>
                  </div>
                </button>
              ))}
          </div>

          <div className="px-4 py-3 border-t border-slate-800">
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                navigate('/notifications');
              }}
              className="w-full text-center text-sm text-brand-300 hover:text-brand-200"
            >
              View all
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
