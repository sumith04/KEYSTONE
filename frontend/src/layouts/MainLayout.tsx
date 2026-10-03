import React, { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { Building2, ClipboardList, Home, LogOut, Menu, Shield, Users, Wrench, X } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { usePermissions } from '../hooks/usePermissions';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
    isActive
      ? 'bg-slate-800 text-white border border-slate-700'
      : 'text-slate-400 hover:text-white hover:bg-slate-800/70'
  }`;

export const MainLayout: React.FC = () => {
  const { user, role, logout } = useAuth();
  const { hasPermission } = usePermissions();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  const showCustomers = hasPermission('VIEW_CUSTOMER');
  const showSites = hasPermission('VIEW_SITE');
  const isTechnician = role === 'TECHNICIAN';
  const showWorkOrders = hasPermission('VIEW_WORK_ORDER') && !isTechnician;
  const showTechnicianWorkspace = hasPermission('VIEW_WORK_ORDER') && isTechnician;
  const displayName = user ? `${user.firstName} ${user.lastName}`.trim() : '';

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
    } finally {
      setLoggingOut(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans">
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-6">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-brand-500/20">
                <Shield className="w-5 h-5 text-white" />
              </div>
              <div>
                <span className="font-bold text-lg tracking-wider text-white">KEYSTONE</span>
                <span className="hidden sm:inline-block ml-2 text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700">
                  FSM Platform
                </span>
              </div>
            </div>

            <nav className="hidden md:flex items-center space-x-2">
              <NavLink to="/" end className={navLinkClass}>
                <span className="inline-flex items-center space-x-2">
                  <Home className="w-4 h-4" />
                  <span>Home</span>
                </span>
              </NavLink>
              {showCustomers && (
                <NavLink to="/customers" className={navLinkClass}>
                  <span className="inline-flex items-center space-x-2">
                    <Users className="w-4 h-4" />
                    <span>Customers</span>
                  </span>
                </NavLink>
              )}
              {showSites && (
                <NavLink to="/sites" className={navLinkClass}>
                  <span className="inline-flex items-center space-x-2">
                    <Building2 className="w-4 h-4" />
                    <span>Sites</span>
                  </span>
                </NavLink>
              )}
              {showWorkOrders && (
                <NavLink to="/work-orders" className={navLinkClass}>
                  <span className="inline-flex items-center space-x-2">
                    <ClipboardList className="w-4 h-4" />
                    <span>Work Orders</span>
                  </span>
                </NavLink>
              )}
              {showTechnicianWorkspace && (
                <NavLink to="/technician" className={navLinkClass}>
                  <span className="inline-flex items-center space-x-2">
                    <Wrench className="w-4 h-4" />
                    <span>Technician Workspace</span>
                  </span>
                </NavLink>
              )}
            </nav>
          </div>

          <div className="flex items-center space-x-3">
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
              className="md:hidden p-2 rounded-lg border border-slate-700 text-slate-300"
              onClick={() => setMobileOpen((open) => !open)}
              aria-label="Toggle navigation"
            >
              {mobileOpen ? <X className="w-4 h-4" /> : <Menu className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {mobileOpen && (
          <nav className="md:hidden border-t border-slate-800 px-4 py-3 space-y-2">
            <NavLink to="/" end className={navLinkClass} onClick={() => setMobileOpen(false)}>
              Home
            </NavLink>
            {showCustomers && (
              <NavLink to="/customers" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                Customers
              </NavLink>
            )}
            {showSites && (
              <NavLink to="/sites" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                Sites
              </NavLink>
            )}
            {showWorkOrders && (
              <NavLink to="/work-orders" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                Work Orders
              </NavLink>
            )}
            {showTechnicianWorkspace && (
              <NavLink to="/technician" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                Technician Workspace
              </NavLink>
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

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>

      <footer className="border-t border-slate-800/80 bg-slate-900/40 py-6 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4">
          &copy; {new Date().getFullYear()} KEYSTONE Field Service Management.
        </div>
      </footer>
    </div>
  );
};
