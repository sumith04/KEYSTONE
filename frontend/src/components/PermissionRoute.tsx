import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { usePermissions } from '../hooks/usePermissions';

interface PermissionRouteProps {
  requiredPermission: string;
  fallbackPath?: string;
}

export const PermissionRoute: React.FC<PermissionRouteProps> = ({
  requiredPermission,
  fallbackPath = '/',
}) => {
  const { hasPermission, loading } = usePermissions();

  if (loading) {
    return (
      <div className="flex items-center justify-center p-8 text-slate-400 text-sm">
        <span>Verifying permissions...</span>
      </div>
    );
  }

  if (!hasPermission(requiredPermission)) {
    return <Navigate to={fallbackPath} replace />;
  }

  return <Outlet />;
};
