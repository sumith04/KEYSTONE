import React from 'react';

interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}

export const Pagination: React.FC<PaginationProps> = ({ page, totalPages, totalElements, onPageChange }) => {
  const safeTotalPages = Math.max(totalPages, 1);

  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pt-4 text-sm text-slate-400">
      <p>
        Showing page {page + 1} of {safeTotalPages} · {totalElements} record{totalElements === 1 ? '' : 's'}
      </p>
      <div className="flex items-center space-x-2">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          className="px-3 py-1.5 rounded-lg border border-slate-700 disabled:opacity-40 hover:bg-slate-800"
        >
          Previous
        </button>
        <button
          type="button"
          disabled={page + 1 >= safeTotalPages}
          onClick={() => onPageChange(page + 1)}
          className="px-3 py-1.5 rounded-lg border border-slate-700 disabled:opacity-40 hover:bg-slate-800"
        >
          Next
        </button>
      </div>
    </div>
  );
};
