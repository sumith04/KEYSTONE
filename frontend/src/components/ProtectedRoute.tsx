import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';

interface ProtectedRouteProps {
  redirectPath?: string;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ redirectPath = '/login' }) => {
  const token = localStorage.getItem('keystone_token');

  if (!token) {
    return <Navigate to={redirectPath} replace />;
  }

  return <Outlet />;
};
