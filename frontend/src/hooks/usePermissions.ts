import { useAuth } from './useAuth';

export const usePermissions = () => {
  const { role, permissions, loading, hasPermission, refreshSession } = useAuth();

  const hasAnyPermission = (requiredPermissions: string[]): boolean => {
    return requiredPermissions.some((permission) => hasPermission(permission));
  };

  return {
    role,
    permissions,
    loading,
    error: null as string | null,
    hasPermission,
    hasAnyPermission,
    refreshPermissions: refreshSession,
  };
};
