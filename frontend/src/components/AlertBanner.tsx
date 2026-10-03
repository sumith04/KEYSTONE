import React from 'react';
import { AlertCircle, CheckCircle2 } from 'lucide-react';

interface AlertBannerProps {
  tone: 'success' | 'error';
  message: string;
  details?: Record<string, string>;
}

export const AlertBanner: React.FC<AlertBannerProps> = ({ tone, message, details }) => {
  const isSuccess = tone === 'success';

  return (
    <div
      className={`rounded-lg border p-4 ${
        isSuccess
          ? 'bg-emerald-950/40 border-emerald-800/60 text-emerald-100'
          : 'bg-rose-950/40 border-rose-800/60 text-rose-100'
      }`}
    >
      <div className="flex items-start space-x-3">
        {isSuccess ? (
          <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
        ) : (
          <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
        )}
        <div className="space-y-2">
          <p className="text-sm font-medium">{message}</p>
          {details && Object.keys(details).length > 0 && (
            <ul className="text-xs space-y-1 text-slate-300">
              {Object.entries(details).map(([field, value]) => (
                <li key={field}>
                  <span className="font-semibold text-slate-200">{field}:</span> {value}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
};
