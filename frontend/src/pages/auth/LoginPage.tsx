import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Lock, Mail, Shield } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { useAuth } from '../../hooks/useAuth';
import { getApiError } from '../../utils/apiError';

const isValidEmail = (value: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [userEmail, setUserEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [success] = useState<string | null>(
    (location.state as { success?: string } | null)?.success || null
  );

  const validate = () => {
    const nextErrors: Record<string, string> = {};
    if (!userEmail.trim()) {
      nextErrors.userEmail = 'Email is required';
    } else if (!isValidEmail(userEmail.trim())) {
      nextErrors.userEmail = 'Please provide a valid email address';
    }
    if (!password) {
      nextErrors.password = 'Password is required';
    }
    setClientErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setApiError(null);
    if (!validate()) {
      return;
    }

    setSubmitting(true);
    try {
      await login(userEmail.trim(), password);
      navigate('/', { replace: true });
    } catch (error) {
      setApiError(getApiError(error).message || 'Invalid email or password');
    } finally {
      setSubmitting(false);
    }
  };

  const fieldClass =
    'w-full rounded-lg border border-slate-700 bg-slate-950 px-10 py-2.5 text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-brand-500';

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 w-12 h-12 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-brand-500/20">
            <Shield className="w-6 h-6 text-white" />
          </div>
          <h1 className="text-2xl font-bold tracking-wide">KEYSTONE</h1>
          <p className="mt-1 text-sm text-slate-400">Field Service Management Platform</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-xl">
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-white">Sign in</h2>
            <p className="text-sm text-slate-400 mt-1">Enter your credentials to continue.</p>
          </div>

          {success && (
            <div className="mb-4">
              <AlertBanner tone="success" message={success} />
            </div>
          )}
          {apiError && <div className="mb-4"><AlertBanner tone="error" message={apiError} /></div>}

          <form onSubmit={handleSubmit} className="space-y-4">
            <label className="block space-y-1 text-sm">
              <span className="text-slate-300">Email</span>
              <div className="relative">
                <Mail className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
                <input
                  type="email"
                  autoComplete="username"
                  value={userEmail}
                  onChange={(event) => setUserEmail(event.target.value)}
                  className={fieldClass}
                  placeholder="you@company.com"
                />
              </div>
              {clientErrors.userEmail && <span className="text-xs text-rose-400">{clientErrors.userEmail}</span>}
            </label>

            <label className="block space-y-1 text-sm">
              <span className="text-slate-300">Password</span>
              <div className="relative">
                <Lock className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  className={`${fieldClass} pr-10`}
                  placeholder="Enter your password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((visible) => !visible)}
                  className="absolute right-3 top-2.5 text-slate-500 hover:text-slate-300"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {clientErrors.password && <span className="text-xs text-rose-400">{clientErrors.password}</span>}
            </label>

            <button
              type="submit"
              disabled={submitting}
              className="w-full mt-2 px-4 py-2.5 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm font-medium disabled:opacity-50"
            >
              {submitting ? 'Signing in...' : 'Login'}
            </button>
          </form>

          <p className="mt-6 text-sm text-center text-slate-400">
            Don't have an account?{' '}
            <Link to="/signup" className="text-brand-300 hover:text-brand-200 font-medium">
              Sign Up
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};
