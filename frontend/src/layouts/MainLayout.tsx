import React from 'react';
import { Outlet } from 'react-router-dom';
import { Shield, Cpu, ExternalLink } from 'lucide-react';

export const MainLayout: React.FC = () => {
  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100 font-sans">
      {/* Top Header */}
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          {/* Logo & Branding */}
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

          {/* Foundation Status Tag */}
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2 text-xs font-medium text-slate-400 bg-slate-800/80 px-3 py-1.5 rounded-lg border border-slate-700/60">
              <Cpu className="w-3.5 h-3.5 text-brand-400" />
              <span>Foundation Phase v0.1.0</span>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 bg-slate-900/40 py-6 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div>
            &copy; {new Date().getFullYear()} KEYSTONE Field Service Management. Base Foundation System.
          </div>
          <div className="flex items-center space-x-4">
            <span className="inline-flex items-center text-slate-400 hover:text-slate-200">
              REST Architecture <ExternalLink className="w-3 h-3 ml-1" />
            </span>
          </div>
        </div>
      </footer>
    </div>
  );
};
