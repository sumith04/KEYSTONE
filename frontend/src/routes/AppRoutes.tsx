import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { PermissionRoute } from '../components/PermissionRoute';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { MainLayout } from '../layouts/MainLayout';
import { CustomerCreatePage } from '../pages/customers/CustomerCreatePage';
import { CustomerDetailPage } from '../pages/customers/CustomerDetailPage';
import { CustomerEditPage } from '../pages/customers/CustomerEditPage';
import { CustomerListPage } from '../pages/customers/CustomerListPage';
import { HomePage } from '../pages/HomePage';
import { SiteCreatePage } from '../pages/sites/SiteCreatePage';
import { SiteDetailPage } from '../pages/sites/SiteDetailPage';
import { SiteEditPage } from '../pages/sites/SiteEditPage';
import { SiteListPage } from '../pages/sites/SiteListPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<HomePage />} />

        <Route element={<ProtectedRoute redirectPath="/" />}>
          <Route element={<PermissionRoute requiredPermission="CREATE_CUSTOMER" />}>
            <Route path="customers/new" element={<CustomerCreatePage />} />
          </Route>
          <Route element={<PermissionRoute requiredPermission="UPDATE_CUSTOMER" />}>
            <Route path="customers/:id/edit" element={<CustomerEditPage />} />
          </Route>
          <Route element={<PermissionRoute requiredPermission="VIEW_CUSTOMER" />}>
            <Route path="customers" element={<CustomerListPage />} />
            <Route path="customers/:id" element={<CustomerDetailPage />} />
          </Route>

          <Route element={<PermissionRoute requiredPermission="CREATE_SITE" />}>
            <Route path="sites/new" element={<SiteCreatePage />} />
          </Route>
          <Route element={<PermissionRoute requiredPermission="UPDATE_SITE" />}>
            <Route path="sites/:id/edit" element={<SiteEditPage />} />
          </Route>
          <Route element={<PermissionRoute requiredPermission="VIEW_SITE" />}>
            <Route path="sites" element={<SiteListPage />} />
            <Route path="sites/:id" element={<SiteDetailPage />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
};
