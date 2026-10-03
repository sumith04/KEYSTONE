import { useState, useEffect } from 'react';
import { fetchUserPermissions, UserPermissionsResponse } from '../services/api';

export const usePermissions = () => {
  const [data, setData] = useState<UserPermissionsResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadPermissions = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchUserPermissions();
      setData(res);
    } catch (err: any) {
      setError(err?.message || 'Failed to fetch user permissions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const token = localStorage.getItem('keystone_token');
    if (token) {
      loadPermissions();
    } else {
      setLoading(false);
    }
  }, []);

  const hasPermission = (requiredPermission: string): boolean => {
    if (!data || !data.permissions) return false;
    return data.permissions.includes(requiredPermission);
  };

  const hasAnyPermission = (requiredPermissions: string[]): boolean => {
    if (!data || !data.permissions) return false;
    return requiredPermissions.some((perm) => data.permissions.includes(perm));
  };

  return {
    role: data?.role || null,
    permissions: data?.permissions || [],
    loading,
    error,
    hasPermission,
    hasAnyPermission,
    refreshPermissions: loadPermissions,
  };
};
