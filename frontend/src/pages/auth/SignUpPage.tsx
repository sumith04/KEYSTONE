import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Lock, Mail, Phone, Shield, UserRound } from 'lucide-react';
import { AlertBanner } from '../../components/AlertBanner';
import { register } from '../../services/api';
import { RegisterRequest, Role } from '../../types';
import { getApiError } from '../../utils/apiError';

const isValidEmail = (value: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

const PUBLIC_ROLES: Array<{ value: Exclude<Role, 'ADMIN'>; label: string }> = [
  { value: 'CUSTOMER', label: 'Customer' },
  { value: 'TECHNICIAN', label: 'Technician' },
  { value: 'DISPATCHER', label: 'Dispatcher' },
  { value: 'MANAGER', label: 'Manager' },
];

export const SignUpPage: React.FC = () => {
  const navigate = useNavigate();
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [userEmail, setUserEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState<Exclude<Role, 'ADMIN'>>('CUSTOMER');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  const validate = () => {
    const nextErrors: Record<string, string> = {};
    if (!firstName.trim()) {
      nextErrors.firstName = 'First name is required';
    }
    if (!lastName.trim()) {
      nextErrors.lastName = 'Last name is required';
    }
    if (!userEmail.trim()) {
      nextErrors.userEmail = 'Email is required';
    } else if (!isValidEmail(userEmail.trim())) {
      nextErrors.userEmail = 'Please provide a valid email address';
    }
    if (!password) {
      nextErrors.password = 'Password is required';
    } else if (password.length < 8) {
      nextErrors.password = 'Password must be at least 8 characters long';
    }
    if (!confirmPassword) {
      nextErrors.confirmPassword = 'Confirm your password';
    } else if (password !== confirmPassword) {
      nextErrors.confirmPassword = 'Passwords do not match';
    }
    if (!role) {
      nextErrors.role = 'Role is required';
    }
    setClientErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setApiError(null);
    setValidationErrors(undefined);
    if (!validate()) {
      return;
    }

    const payload: RegisterRequest = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      userEmail: userEmail.trim(),
      password,
      role,
    };
    if (phone.trim()) {
      payload.phone = phone.trim();
    }

    setSubmitting(true);
    try {
      await register(payload);
      navigate('/login', {
        replace: true,
        state: { success: 'Account created successfully. Sign in to continue.' },
      });
    } catch (error) {
      const api = getApiError(error);
      setApiError(api.message || 'Unable to create the account');
      setValidationErrors(api.validationErrors);
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
            <h2 className="text-lg font-semibold text-white">Create an account</h2>
            <p className="text-sm text-slate-400 mt-1">Register to access the KEYSTONE workspace.</p>
          </div>

          {apiError && (
            <div className="mb-4">
              <AlertBanner tone="error" message={apiError} details={validationErrors} />
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <label className="block space-y-1 text-sm">
                <span className="text-slate-300">First name</span>
                <div className="relative">
                  <UserRound className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
                  <input
                    value={firstName}
                    onChange={(event) => setFirstName(event.target.value)}
                    className={fieldClass}
                    autoComplete="given-name"
                    placeholder="Jane"
                  />
                </div>
                {clientErrors.firstName && <span className="text-xs text-rose-400">{clientErrors.firstName}</span>}
              </label>
              <label className="block space-y-1 text-sm">
                <span className="text-slate-300">Last name</span>
                <div className="relative">
                  <UserRound className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
                  <input
                    value={lastName}
                    onChange={(event) => setLastName(event.target.value)}
                    className={fieldClass}
                    autoComplete="family-name"
                    placeholder="Smith"
                  />
                </div>
                {clientErrors.lastName && <span className="text-xs text-rose-400">{clientErrors.lastName}</span>}
              </label>
            </div>

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
              <span className="text-slate-300">Phone <span className="text-slate-500">(optional)</span></span>
              <div className="relative">
                <Phone className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
                <input
                  type="tel"
                  autoComplete="tel"
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  className={fieldClass}
                  placeholder="555-0100"
                />
              </div>
            </label>

            <label className="block space-y-1 text-sm">
              <span className="text-slate-300">Role</span>
              <select
                value={role}
                onChange={(event) => setRole(event.target.value as Exclude<Role, 'ADMIN'>)}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-slate-100 focus:outline-none focus:ring-2 focus:ring-brand-500"
              >
                {PUBLIC_ROLES.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              {clientErrors.role && <span className="text-xs text-rose-400">{clientErrors.role}</span>}
            </label>

            <label className="block space-y-1 text-sm">
              <span className="text-slate-300">Password</span>
              <div className="relative">
                <Lock className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  className={`${fieldClass} pr-10`}
                  placeholder="At least 8 characters"
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

            <label className="block space-y-1 text-sm">
              <span className="text-slate-300">Confirm password</span>
              <div className="relative">
                <Lock className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  className={fieldClass}
                  placeholder="Re-enter your password"
                />
              </div>
              {clientErrors.confirmPassword && (
                <span className="text-xs text-rose-400">{clientErrors.confirmPassword}</span>
              )}
            </label>

            <button
              type="submit"
              disabled={submitting}
              className="w-full mt-2 px-4 py-2.5 rounded-lg bg-brand-600 hover:bg-brand-500 text-white text-sm font-medium disabled:opacity-50"
            >
              {submitting ? 'Creating account...' : 'Sign Up'}
            </button>
          </form>

          <p className="mt-6 text-sm text-center text-slate-400">
            Already have an account?{' '}
            <Link to="/login" className="text-brand-300 hover:text-brand-200 font-medium">
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};
