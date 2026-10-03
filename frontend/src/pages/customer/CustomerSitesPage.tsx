import React, { useEffect, useState } from 'react';
import { AlertBanner } from '../../components/AlertBanner';
import { Pagination } from '../../components/Pagination';
import { StatusBadge } from '../../components/StatusBadge';
import { getCustomerSites } from '../../services/api';
import { SitePageResponse } from '../../types';
import { getApiError } from '../../utils/apiError';

export const CustomerSitesPage: React.FC = () => {
  const [pageData, setPageData] = useState<SitePageResponse | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getCustomerSites({ page, size: 10 })
      .then(setPageData)
      .catch((err) => setError(getApiError(err).message || 'Unable to load sites'))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">My Sites</h1>
        <p className="text-sm text-slate-400">Facilities linked to your customer account. Site details are read-only.</p>
      </div>

      {error && <AlertBanner tone="error" message={error} />}

      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-400">Loading sites...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-6 text-sm text-slate-400">No sites are linked to your account yet.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-950/70 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Site</th>
                  <th className="px-4 py-3">Address</th>
                  <th className="px-4 py-3">Contact</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((site) => (
                  <tr key={site.id} className="border-t border-slate-800">
                    <td className="px-4 py-3">
                      <p className="font-medium text-white">{site.siteName}</p>
                      <p className="text-xs text-slate-500">{site.siteCode}</p>
                    </td>
                    <td className="px-4 py-3 text-slate-300">
                      {[site.addressLine1, site.city, site.state, site.postalCode, site.country]
                        .filter(Boolean)
                        .join(', ') || '—'}
                    </td>
                    <td className="px-4 py-3 text-slate-300">
                      <p>{site.contactName || '—'}</p>
                      <p className="text-xs text-slate-500">{site.contactPhone || site.contactEmail || ''}</p>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={site.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {pageData && (
        <Pagination
          page={pageData.page}
          totalPages={pageData.totalPages}
          totalElements={pageData.totalElements}
          onPageChange={setPage}
        />
      )}
    </div>
  );
};
