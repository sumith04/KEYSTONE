import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { fetchSystemHealth, SystemHealthResponse } from '../services/api';
import { CheckCircle2, AlertCircle, RefreshCw, Server, Database, Layers, ArrowRight } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

export const HomePage: React.FC = () => {
  const { role } = useAuth();
  const [health, setHealth] = useState<SystemHealthResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const checkBackendHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchSystemHealth();
      setHealth(data);
    } catch (err: any) {
      setError(err?.message || 'Unable to connect to Spring Boot backend service');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkBackendHealth();
  }, []);

  if (role === 'CUSTOMER') {
    return <Navigate to="/customer" replace />;
  }

  return (
    <div className="space-y-8">
      {/* Hero / System Status Header */}
      <section className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 sm:p-8 relative overflow-hidden">
        <div className="absolute -right-10 -bottom-10 w-72 h-72 bg-brand-500/10 rounded-full blur-3xl pointer-events-none" />
        
        <div className="max-w-3xl space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-brand-950 text-brand-300 text-xs font-semibold border border-brand-800/50">
            <span className="w-2 h-2 rounded-full bg-brand-400 animate-pulse" />
            <span>Commercial Field Service Management</span>
          </div>
          
          <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            KEYSTONE Project Foundation
          </h1>
          
          <p className="text-slate-400 text-base leading-relaxed">
            Scalable enterprise project foundation initialized with Spring Boot backend, React + TypeScript + Vite frontend, and PostgreSQL database configuration.
          </p>
        </div>
      </section>

      {/* Backend API Connectivity Verification Card */}
      <section className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-lg bg-slate-800 text-brand-400 border border-slate-700">
              <Server className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-white">Backend Health Endpoint</h2>
              <p className="text-xs text-slate-400">Communicating with <code className="text-brand-300">/api/health</code></p>
            </div>
          </div>

          <button
            onClick={checkBackendHealth}
            disabled={loading}
            className="inline-flex items-center justify-center space-x-2 px-4 py-2 text-xs font-medium rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 transition-colors border border-slate-700 disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Test Endpoint</span>
          </button>
        </div>

        <div className="pt-6">
          {loading ? (
            <div className="flex items-center space-x-3 text-slate-400 text-sm py-4">
              <RefreshCw className="w-4 h-4 animate-spin text-brand-400" />
              <span>Checking Spring Boot REST endpoint...</span>
            </div>
          ) : error ? (
            <div className="bg-amber-950/40 border border-amber-800/60 rounded-lg p-4 flex items-start space-x-3">
              <AlertCircle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
              <div className="text-xs space-y-1">
                <p className="font-semibold text-amber-200">Backend Standby / Connection Pending</p>
                <p className="text-amber-300/80">{error}</p>
                <p className="text-slate-400 mt-2">
                  Start the Spring Boot backend service using <code className="text-slate-200 bg-slate-800 px-1.5 py-0.5 rounded">mvn spring-boot:run</code> in the <code className="text-slate-200 bg-slate-800 px-1.5 py-0.5 rounded">backend/</code> directory.
                </p>
              </div>
            </div>
          ) : health ? (
            <div className="bg-emerald-950/40 border border-emerald-800/60 rounded-lg p-4 flex items-start justify-between">
              <div className="flex items-start space-x-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-sm text-emerald-200">REST API Status: {health.status}</span>
                    <span className="px-2 py-0.5 rounded text-[10px] uppercase font-bold bg-emerald-900/60 text-emerald-300 border border-emerald-700">Online</span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">{health.application}</p>
                  <p className="text-[11px] text-slate-500 font-mono mt-0.5">Response Time: {health.timestamp}</p>
                </div>
              </div>
            </div>
          ) : null}
        </div>
      </section>

      {/* Stack Architecture Highlights */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-5 space-y-3">
          <div className="w-9 h-9 rounded-lg bg-indigo-950 text-indigo-400 flex items-center justify-center border border-indigo-800/60">
            <Server className="w-5 h-5" />
          </div>
          <h3 className="font-semibold text-white text-base">Backend Architecture</h3>
          <ul className="text-xs text-slate-400 space-y-1.5 list-disc list-inside">
            <li>Java 21 & Spring Boot 3.3.4</li>
            <li>REST Controller Package Separation</li>
            <li>Spring Web & Spring Data JPA</li>
            <li>Environment Variable Config</li>
          </ul>
        </div>

        <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-5 space-y-3">
          <div className="w-9 h-9 rounded-lg bg-cyan-950 text-cyan-400 flex items-center justify-center border border-cyan-800/60">
            <Layers className="w-5 h-5" />
          </div>
          <h3 className="font-semibold text-white text-base">Frontend Architecture</h3>
          <ul className="text-xs text-slate-400 space-y-1.5 list-disc list-inside">
            <li>React 18 & TypeScript</li>
            <li>Vite Development Server</li>
            <li>React Router Navigation</li>
            <li>Tailwind CSS Design System</li>
          </ul>
        </div>

        <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-5 space-y-3">
          <div className="w-9 h-9 rounded-lg bg-blue-950 text-blue-400 flex items-center justify-center border border-blue-800/60">
            <Database className="w-5 h-5" />
          </div>
          <h3 className="font-semibold text-white text-base">Database Configuration</h3>
          <ul className="text-xs text-slate-400 space-y-1.5 list-disc list-inside">
            <li>PostgreSQL Relational Driver</li>
            <li>JDBC URL & Credential Mapping</li>
            <li>Environment Variable Binding</li>
            <li>JPA Hibernate ORM Layer</li>
          </ul>
        </div>
      </section>

      {/* Planned Business Modules Notice */}
      <section className="bg-slate-900/40 border border-slate-800/80 rounded-xl p-6">
        <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">
          Planned Platform Business Modules (Future Phases)
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 text-xs text-slate-400">
          {[
            'Authentication (JWT)',
            'RBAC Security',
            'User & Role Management',
            'Customer Portals',
            'Facility Sites',
            'Service Requests',
            'Work Order Lifecycle',
            'Technician Dispatching',
            'Parts & Inventory',
            'Time Tracking',
            'SLA Monitoring',
            'Real-Time WebSockets',
            'Analytical Dashboards',
            'Custom Reports',
            'Audit Logging'
          ].map((item, idx) => (
            <div key={idx} className="flex items-center space-x-2 bg-slate-900 px-3 py-2 rounded-lg border border-slate-800">
              <ArrowRight className="w-3 h-3 text-slate-600 shrink-0" />
              <span className="truncate">{item}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};
