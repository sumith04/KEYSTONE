import React, { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { Bell, Building2, ClipboardList, Inbox, LayoutDashboard, LogOut, Menu, Shield, UserRound, X } from 'lucide-react';
import { NotificationBell } from '../components/NotificationBell';
import { useAuth } from '../hooks/useAuth';
import { usePermissions } from '../hooks/usePermissions';
import { internalModuleNav, isModuleVisible } from '../utils/navigation';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
    isActive
      ? 'bg-slate-800 text-white border border-slate-700'
      : 'text-slate-400 hover:text-white hover:bg-slate-800/70'
  }`;

const sidebarLinkClass = ({ isActive }: { isActive: boolean }) =>
  `flex items-center space-x-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
    isActive
      ? 'bg-slate-800 text-white border border-slate-700'
      : 'text-slate-400 hover:text-white hover:bg-slate-800/70 border border-transparent'
  }`;

export const MainLayout: React.FC = () => {
  const { user, role, logout } = useAuth();
  const { hasPermission } = usePermissions();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  const isCustomer = role === 'CUSTOMER';
  const visibleModules = internalModuleNav.filter((item) => isModuleVisible(item, role, hasPermission));
  const displayName = user ? `${user.firstName} ${user.lastName}`.trim() : '';

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
    } finally {
      setLoggingOut(false);
    }
  };

  const customerLinks = (
    <>
      <NavLink to="/customer" end className={navLinkClass} onClick={() => setMobileOpen(false)}>
        <span className="inline-flex items-center space-x-2">
          <LayoutDashboard className="w-4 h-4" />
          <span>Dashboard</span>
        </span>
      </NavLink>
      <NavLink to="/customer/requests" className={navLinkClass} onClick={() => setMobileOpen(false)}>
        <span className="inline-flex items-center space-x-2">
          <Inbox className="w-4 h-4" />
          <span>My Requests</span>
        </span>
      </NavLink>
      <NavLink to="/customer/work-orders" className={navLinkClass} onClick={() => setMobileOpen(false)}>
        <span className="inline-flex items-center space-x-2">
          <ClipboardList className="w-4 h-4" />
          <span>My Work Orders</span>
        </span>
      </NavLink>
      <NavLink to="/customer/sites" className={navLinkClass} onClick={() => setMobileOpen(false)}>
        <span className="inline-flex items-center space-x-2">
          <Building2 className="w-4 h-4" />
          <span>My Sites</span>
        </span>
      </NavLink>
      <NavLink to="/customer/profile" className={navLinkClass} onClick={() => setMobileOpen(false)}>
        <span className="inline-flex items-center space-x-2">
          <UserRound className="w-4 h-4" />
          <span>Profile</span>
        </span>
      </NavLink>
      <NavLink to="/notifications" className={navLinkClass} onClick={() => setMobileOpen(false)}>
        <span className="inline-flex items-center space-x-2">
          <Bell className="w-4 h-4" />
          <span>Notifications</span>
        </span>
      </NavLink>
    </>
  );

  const internalNavLinks = visibleModules.map((item) => (
    <NavLink key={item.to} to={item.to} className={sidebarLinkClass} onClick={() => setMobileOpen(false)}>
      <item.icon className="w-4 h-4 shrink-0" />
      <span>{item.label}</span>
    </NavLink>
  ));

  return (
    <div className="min-h-screen flex bg-slate-950 text-slate-100 font-sans">
      {!isCustomer && (
        <aside className="hidden lg:flex w-64 shrink-0 flex-col border-r border-slate-800 bg-slate-900/80 sticky top-0 h-screen">
          <div className="h-16 px-4 flex items-center space-x-3 border-b border-slate-800">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-brand-500/20">
              <Shield className="w-5 h-5 text-white" />
            </div>
            <div>
              <span className="font-bold text-lg tracking-wider text-white">KEYSTONE</span>
              <p className="text-[11px] uppercase tracking-wide text-slate-500">FSM Platform</p>
            </div>
          </div>
          <nav className="flex-1 overflow-y-auto p-3 space-y-1">
            <NavLink to="/" end className={sidebarLinkClass}>
              <LayoutDashboard className="w-4 h-4 shrink-0" />
              <span>Home</span>
            </NavLink>
            {internalNavLinks}
          </nav>
        </aside>
      )}

      <div className="flex-1 flex flex-col min-w-0">
        <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-50">
          <div className="px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
            <div className="flex items-center space-x-6 min-w-0">
              <div className={`flex items-center space-x-3 ${isCustomer ? '' : 'lg:hidden'}`}>
                <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-brand-500/20">
                  <Shield className="w-5 h-5 text-white" />
                </div>
                <div>
                  <span className="font-bold text-lg tracking-wider text-white">KEYSTONE</span>
                  <span className="hidden sm:inline-block ml-2 text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700">
                    {isCustomer ? 'Customer Portal' : 'FSM Platform'}
                  </span>
                </div>
              </div>

              {isCustomer && <nav className="hidden md:flex items-center space-x-2">{customerLinks}</nav>}
            </div>

            <div className="flex items-center space-x-3">
              <NotificationBell />
              <div className="hidden sm:block text-right">
                <p className="text-xs font-medium text-slate-200">{displayName || user?.userEmail}</p>
                {role && <p className="text-[11px] uppercase tracking-wide text-slate-500">{role}</p>}
              </div>
              <button
                type="button"
                onClick={handleLogout}
                disabled={loggingOut}
                className="hidden md:inline-flex items-center space-x-2 px-3 py-2 rounded-lg border border-slate-700 text-slate-300 hover:bg-slate-800 text-sm disabled:opacity-50"
              >
                <LogOut className="w-4 h-4" />
                <span>{loggingOut ? 'Signing out...' : 'Logout'}</span>
              </button>
              <button
                type="button"
                className={`${isCustomer ? 'md:hidden' : 'lg:hidden'} p-2 rounded-lg border border-slate-700 text-slate-300`}
                onClick={() => setMobileOpen((open) => !open)}
                aria-label="Toggle navigation"
              >
                {mobileOpen ? <X className="w-4 h-4" /> : <Menu className="w-4 h-4" />}
              </button>
            </div>
          </div>

          {mobileOpen && (
            <nav className="border-t border-slate-800 px-4 py-3 space-y-2">
              {isCustomer ? (
                customerLinks
              ) : (
                <>
                  <NavLink to="/" end className={sidebarLinkClass} onClick={() => setMobileOpen(false)}>
                    <LayoutDashboard className="w-4 h-4 shrink-0" />
                    <span>Home</span>
                  </NavLink>
                  {internalNavLinks}
                </>
              )}
              <div className="pt-2 border-t border-slate-800">
                <p className="px-3 py-1 text-xs text-slate-400">{displayName || user?.userEmail}</p>
                <button
                  type="button"
                  onClick={handleLogout}
                  disabled={loggingOut}
                  className="w-full mt-2 inline-flex items-center justify-center space-x-2 px-3 py-2 rounded-lg border border-slate-700 text-slate-300 hover:bg-slate-800 text-sm disabled:opacity-50"
                >
                  <LogOut className="w-4 h-4" />
                  <span>{loggingOut ? 'Signing out...' : 'Logout'}</span>
                </button>
              </div>
            </nav>
          )}
        </header>

        <main className="flex-1 w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 max-w-7xl">
          <Outlet />
        </main>

        <footer className="border-t border-slate-800/80 bg-slate-900/40 py-6 text-center text-xs text-slate-500">
          <div className="max-w-7xl mx-auto px-4">
            &copy; {new Date().getFullYear()} KEYSTONE Field Service Management.
          </div>
        </footer>
      </div>
    </div>
  );
};
