import React, { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AUTH_TOKEN_KEY,
  AUTH_UNAUTHORIZED_EVENT,
  AUTH_USER_KEY,
  fetchCurrentUser,
  fetchUserPermissions,
  login as loginRequest,
  logout as logoutRequest,
} from '../services/api';
import { AuthUser, Role } from '../types';

interface AuthContextValue {
  user: AuthUser | null;
  role: Role | null;
  permissions: string[];
  isAuthenticated: boolean;
  loading: boolean;
  login: (userEmail: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasPermission: (permission: string) => boolean;
  refreshSession: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

interface AuthProviderProps {
  children: React.ReactNode;
}

const clearStoredAuth = () => {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
};

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const navigate = useNavigate();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  const applySession = useCallback(async () => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    if (!token) {
      setUser(null);
      setPermissions([]);
      return;
    }

    const [currentUser, permissionResponse] = await Promise.all([
      fetchCurrentUser(),
      fetchUserPermissions(),
    ]);

    setUser(currentUser);
    setPermissions(permissionResponse.permissions || []);
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(currentUser));
  }, []);

  const refreshSession = useCallback(async () => {
    setLoading(true);
    try {
      await applySession();
    } catch {
      clearStoredAuth();
      setUser(null);
      setPermissions([]);
    } finally {
      setLoading(false);
    }
  }, [applySession]);

  useEffect(() => {
    refreshSession();
  }, [refreshSession]);

  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
      setPermissions([]);
      if (window.location.pathname !== '/login') {
        navigate('/login', { replace: true });
      }
    };

    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
    return () => window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized);
  }, [navigate]);

  const login = useCallback(async (userEmail: string, password: string) => {
    const response = await loginRequest({ userEmail, password });
    localStorage.setItem(AUTH_TOKEN_KEY, response.accessToken);
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(response.user));
    setUser(response.user);

    const [currentUser, permissionResponse] = await Promise.all([
      fetchCurrentUser(),
      fetchUserPermissions(),
    ]);

    setUser(currentUser);
    setPermissions(permissionResponse.permissions || []);
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(currentUser));
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutRequest();
    } catch {
      // Local session must still be cleared if the API call fails.
    } finally {
      clearStoredAuth();
      setUser(null);
      setPermissions([]);
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  const hasPermission = useCallback(
    (permission: string) => permissions.includes(permission),
    [permissions]
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      role: user?.role ?? null,
      permissions,
      isAuthenticated: Boolean(user && localStorage.getItem(AUTH_TOKEN_KEY)),
      loading,
      login,
      logout,
      hasPermission,
      refreshSession,
    }),
    [user, permissions, loading, login, logout, hasPermission, refreshSession]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
