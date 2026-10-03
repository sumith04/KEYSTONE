import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertBanner } from '../../components/AlertBanner';
import { getCustomers, getSite, updateSite } from '../../services/api';
import { CreateSiteRequest, Customer, Site } from '../../types';
import { getApiError } from '../../utils/apiError';
import { SiteForm } from './SiteForm';

export const SiteEditPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const siteId = Number(id);
  const [site, setSite] = useState<Site | null>(null);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string> | undefined>();

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const [siteData, customerData] = await Promise.all([
          getSite(siteId),
          getCustomers({ page: 0, size: 100, sort: 'companyName' }),
        ]);
        setSite(siteData);
        setCustomers(customerData.content);
      } catch (err) {
        setError(getApiError(err).message || 'Unable to load site');
      } finally {
        setLoading(false);
      }
    };

    if (Number.isFinite(siteId)) {
      load();
    } else {
      setError('Invalid site id');
      setLoading(false);
    }
  }, [siteId]);

  const handleSubmit = async (values: CreateSiteRequest) => {
    setSubmitting(true);
    setError(null);
    setValidationErrors(undefined);
    try {
      const updated = await updateSite(siteId, values);
      navigate(`/sites/${updated.id}`, {
        state: { success: `Site ${updated.siteCode} was updated.` },
      });
    } catch (err) {
      const apiError = getApiError(err);
      setError(apiError.message || 'Unable to update site');
      setValidationErrors(apiError.validationErrors);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-slate-400">Loading site...</div>;
  }

  if (!site) {
    return <AlertBanner tone="error" message={error || 'Site not found.'} />;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-white">Edit site</h1>
        <p className="text-sm text-slate-400">{site.siteCode}</p>
      </div>
      {error && <AlertBanner tone="error" message={error} details={validationErrors} />}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <SiteForm
          initialValues={{
            siteCode: site.siteCode,
            siteName: site.siteName,
            customerId: site.customerId,
            addressLine1: site.addressLine1 ?? undefined,
            addressLine2: site.addressLine2 ?? undefined,
            city: site.city ?? undefined,
            state: site.state ?? undefined,
            postalCode: site.postalCode ?? undefined,
            country: site.country ?? undefined,
            contactName: site.contactName ?? undefined,
            contactPhone: site.contactPhone ?? undefined,
            contactEmail: site.contactEmail ?? undefined,
            description: site.description ?? undefined,
          }}
          customers={customers}
          submitLabel="Save changes"
          submitting={submitting}
          onSubmit={handleSubmit}
          onCancel={() => navigate(`/sites/${site.id}`)}
        />
      </div>
    </div>
  );
};
