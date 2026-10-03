import React, { useEffect, useState } from 'react';
import { AlertBanner } from '../../components/AlertBanner';
import { StatusBadge } from '../../components/StatusBadge';
import { getCustomerProfile } from '../../services/api';
import { CustomerProfile } from '../../types';
import { getApiError } from '../../utils/apiError';

export const CustomerProfilePage: React.FC = () => {
  const [profile, setProfile] = useState<CustomerProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getCustomerProfile()
      .then(setProfile)
      .catch((err) => setError(getApiError(err).message || 'Unable to load profile'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return <p className="text-sm text-slate-400">Loading profile...</p>;
  }

  if (error && !profile) {
    return <AlertBanner tone="error" message={error} />;
  }

  if (!profile) {
    return <AlertBanner tone="error" message="Profile not found." />;
  }

  const contactName = [profile.contactFirstName, profile.contactLastName].filter(Boolean).join(' ') || '—';
  const address = [
    profile.addressLine1,
    profile.addressLine2,
    profile.city,
    profile.state,
    profile.postalCode,
    profile.country,
  ]
    .filter(Boolean)
    .join(', ') || '—';

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Profile</h1>
        <p className="text-sm text-slate-400">Account details for your customer organization. Contact KEYSTONE to request changes.</p>
      </div>

      <section className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-5">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">Company</p>
            <h2 className="text-xl font-semibold text-white">{profile.companyName}</h2>
            <p className="text-sm text-slate-400">{profile.customerCode}</p>
          </div>
          <StatusBadge status={profile.status} />
        </div>
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500">Contact name</dt>
            <dd className="text-slate-200">{contactName}</dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500">Email</dt>
            <dd className="text-slate-200">{profile.email || '—'}</dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500">Phone</dt>
            <dd className="text-slate-200">{profile.phone || '—'}</dd>
          </div>
          <div>
            <dt className="text-xs uppercase tracking-wide text-slate-500">Alternate phone</dt>
            <dd className="text-slate-200">{profile.alternatePhone || '—'}</dd>
          </div>
          <div className="sm:col-span-2">
            <dt className="text-xs uppercase tracking-wide text-slate-500">Address</dt>
            <dd className="text-slate-200">{address}</dd>
          </div>
        </dl>
      </section>
    </div>
  );
};
