import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { Pagination } from '../../components/Pagination';
import { useAuth } from '../../hooks/useAuth';
import { useNotifications } from '../../hooks/useNotifications';
import { getNotifications } from '../../services/api';
import { Notification, NotificationPage } from '../../types';
import { getApiError } from '../../utils/apiError';
import { formatRelativeTime, notificationPath, notificationTypeLabel } from '../../utils/notifications';

type ReadFilter = 'all' | 'unread';

export const NotificationsPage: React.FC = () => {
  const navigate = useNavigate();
  const { role } = useAuth();
  const { unreadCount, markRead, markAllRead, refreshUnreadCount } = useNotifications();
  const [pageData, setPageData] = useState<NotificationPage | null>(null);
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<ReadFilter>('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [markingAll, setMarkingAll] = useState(false);

  const loadPage = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getNotifications({
        page,
        size: 10,
        read: filter === 'unread' ? false : undefined,
      });
      setPageData(data);
    } catch (err) {
      setError(getApiError(err).message || 'Unable to load notifications');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, filter]);

  const handleFilterChange = (next: ReadFilter) => {
    setPage(0);
    setFilter(next);
  };

  const handleOpen = async (notification: Notification) => {
    try {
      if (!notification.read) {
        await markRead(notification.id);
      }
    } catch {
      // Continue navigation even if the read update fails.
    }
    const path = notificationPath(notification.relatedEntityType, notification.relatedEntityId, role);
    if (path) {
      navigate(path);
      return;
    }
    loadPage();
    refreshUnreadCount().catch(() => undefined);
  };

  const handleMarkAll = async () => {
    setMarkingAll(true);
    setError(null);
    try {
      await markAllRead();
      await loadPage();
    } catch (err) {
      setError(getApiError(err).message || 'Unable to mark notifications as read');
    } finally {
      setMarkingAll(false);
    }
  };

  const displayedUnread = pageData?.unreadCount ?? unreadCount;

  return (
    <div className="space-y-6">
      <section className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-start space-x-3">
            <div className="p-2.5 rounded-lg bg-slate-800 text-brand-400 border border-slate-700">
              <Bell className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">Notifications</h1>
              <p className="text-sm text-slate-400 mt-1">
                {displayedUnread} unread notification{displayedUnread === 1 ? '' : 's'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleMarkAll}
            disabled={markingAll || displayedUnread === 0}
            className="inline-flex items-center justify-center space-x-2 px-3 py-2 rounded-lg border border-slate-700 text-slate-200 hover:bg-slate-800 text-sm disabled:opacity-40"
          >
            <CheckCheck className="w-4 h-4" />
            <span>{markingAll ? 'Updating...' : 'Mark all as read'}</span>
          </button>
        </div>
      </section>

      <div className="flex items-center space-x-2">
        <button
          type="button"
          onClick={() => handleFilterChange('all')}
          className={`px-3 py-1.5 rounded-lg text-sm border ${
            filter === 'all'
              ? 'bg-slate-800 text-white border-slate-600'
              : 'border-slate-800 text-slate-400 hover:bg-slate-800/60'
          }`}
        >
          All
        </button>
        <button
          type="button"
          onClick={() => handleFilterChange('unread')}
          className={`px-3 py-1.5 rounded-lg text-sm border ${
            filter === 'unread'
              ? 'bg-slate-800 text-white border-slate-600'
              : 'border-slate-800 text-slate-400 hover:bg-slate-800/60'
          }`}
        >
          Unread
        </button>
      </div>

      {error && <AlertBanner tone="error" message={error} />}

      <section className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {loading && <p className="px-4 py-8 text-sm text-slate-500">Loading notifications...</p>}
        {!loading && pageData && pageData.content.length === 0 && (
          <p className="px-4 py-8 text-sm text-slate-500">
            {filter === 'unread' ? 'You have no unread notifications.' : 'No notifications yet.'}
          </p>
        )}
        {!loading &&
          pageData?.content.map((notification) => (
            <button
              key={notification.id}
              type="button"
              onClick={() => handleOpen(notification)}
              className={`w-full text-left px-4 py-4 border-b border-slate-800 last:border-b-0 hover:bg-slate-800/50 ${
                notification.read ? 'opacity-70' : 'bg-slate-800/20'
              }`}
            >
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <div className="flex items-center space-x-2">
                    <p className="text-sm font-semibold text-white">{notification.title}</p>
                    {!notification.read && <span className="w-2 h-2 rounded-full bg-brand-400" />}
                  </div>
                  <p className="text-sm text-slate-400 mt-1">{notification.message}</p>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-[11px] uppercase tracking-wide text-slate-500">
                    {notificationTypeLabel(notification.type)}
                  </p>
                  <p className="text-xs text-slate-500 mt-1">{formatRelativeTime(notification.createdAt)}</p>
                </div>
              </div>
            </button>
          ))}
      </section>

      {pageData && (
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onPageChange={setPage}
        />
      )}
    </div>
  );
};
