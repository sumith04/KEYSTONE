import React, { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { Building2, Home, Menu, Shield, Users, X } from 'lucide-react';
import { usePermissions } from '../hooks/usePermissions';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
    isActive
      ? 'bg-slate-800 text-white border border-slate-700'
      : 'text-slate-400 hover:text-white hover:bg-slate-800/70'
  }`;

export const MainLayout: React.FC = () => {
  const { hasPermission } = usePermissions();
  const [mobileOpen, setMobileOpen] = useState(false);

  const showCustomers = hasPermission('VIEW_CUSTOMER');
  const showSites = hasPermission('VIEW_SITE');

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
            </nav>
          </div>

          <button
            type="button"
            className="md:hidden p-2 rounded-lg border border-slate-700 text-slate-300"
            onClick={() => setMobileOpen((open) => !open)}
            aria-label="Toggle navigation"
          >
            {mobileOpen ? <X className="w-4 h-4" /> : <Menu className="w-4 h-4" />}
          </button>
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
