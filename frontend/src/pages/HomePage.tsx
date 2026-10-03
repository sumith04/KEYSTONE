import React from 'react';
import { Link, Navigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { usePermissions } from '../hooks/usePermissions';
import { internalModuleNav, isModuleVisible } from '../utils/navigation';

export const HomePage: React.FC = () => {
  const { user, role } = useAuth();
  const { hasPermission } = usePermissions();

  if (role === 'CUSTOMER') {
    return <Navigate to="/customer" replace />;
  }

  const modules = internalModuleNav.filter((item) => isModuleVisible(item, role, hasPermission));
  const displayName = user ? `${user.firstName} ${user.lastName}`.trim() || user.userEmail : '';

  return (
    <div className="space-y-8">
      <section className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 sm:p-8 relative overflow-hidden">
        <div className="absolute -right-10 -bottom-10 w-72 h-72 bg-brand-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="max-w-3xl space-y-3 relative">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-brand-950 text-brand-300 text-xs font-semibold border border-brand-800/50">
            <span className="w-2 h-2 rounded-full bg-brand-400" />
            <span>Field Service Management</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Welcome{displayName ? `, ${displayName}` : ''}
          </h1>
          <p className="text-slate-400 text-base leading-relaxed">
            Open a KEYSTONE module to manage customers, sites, work orders, inventory, SLAs, and incoming
            service requests.
          </p>
        </div>
      </section>

      <section>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider">Modules</h2>
          {role && <p className="text-xs uppercase tracking-wide text-slate-500">{role}</p>}
        </div>
        {modules.length === 0 ? (
          <p className="text-sm text-slate-500">No modules are available for this account.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
            {modules.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className="group bg-slate-900 border border-slate-800 rounded-xl p-5 hover:border-slate-600 hover:bg-slate-800/40 transition-colors"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="w-10 h-10 rounded-lg bg-slate-800 text-brand-300 border border-slate-700 flex items-center justify-center">
                    <item.icon className="w-5 h-5" />
                  </div>
                  <ArrowRight className="w-4 h-4 text-slate-600 group-hover:text-brand-300 transition-colors" />
                </div>
                <h3 className="mt-4 text-base font-semibold text-white">{item.label}</h3>
                <p className="mt-1 text-sm text-slate-400">{item.description}</p>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
