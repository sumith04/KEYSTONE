import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { PermissionRoute } from '../components/PermissionRoute';
import { GuestRoute, ProtectedRoute } from '../components/ProtectedRoute';
import { MainLayout } from '../layouts/MainLayout';
import { LoginPage } from '../pages/auth/LoginPage';
import { CustomerCreatePage } from '../pages/customers/CustomerCreatePage';
import { CustomerDetailPage } from '../pages/customers/CustomerDetailPage';
import { CustomerEditPage } from '../pages/customers/CustomerEditPage';
import { CustomerListPage } from '../pages/customers/CustomerListPage';
import { HomePage } from '../pages/HomePage';
import { SiteCreatePage } from '../pages/sites/SiteCreatePage';
import { SiteDetailPage } from '../pages/sites/SiteDetailPage';
import { SiteEditPage } from '../pages/sites/SiteEditPage';
import { SiteListPage } from '../pages/sites/SiteListPage';
import { PartCreatePage } from '../pages/parts/PartCreatePage';
import { PartEditPage } from '../pages/parts/PartEditPage';
import { PartsPage } from '../pages/parts/PartsPage';
import { TechnicianWorkspacePage } from '../pages/technician/TechnicianWorkspacePage';
import { WorkOrderCreatePage } from '../pages/work-orders/WorkOrderCreatePage';
import { WorkOrderDetailsPage } from '../pages/work-orders/WorkOrderDetailsPage';
import { WorkOrderEditPage } from '../pages/work-orders/WorkOrderEditPage';
import { WorkOrdersPage } from '../pages/work-orders/WorkOrdersPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route element={<GuestRoute />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<HomePage />} />

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

          <Route element={<PermissionRoute requiredPermission="CREATE_WORK_ORDER" />}>
            <Route path="work-orders/new" element={<WorkOrderCreatePage />} />
          </Route>
          <Route element={<PermissionRoute requiredPermission="UPDATE_WORK_ORDER" />}>
            <Route path="work-orders/:id/edit" element={<WorkOrderEditPage />} />
          </Route>
          <Route element={<PermissionRoute requiredPermission="VIEW_WORK_ORDER" />}>
            <Route path="technician" element={<TechnicianWorkspacePage />} />
            <Route path="work-orders" element={<WorkOrdersPage />} />
            <Route path="work-orders/:id" element={<WorkOrderDetailsPage />} />
          </Route>

          <Route element={<PermissionRoute requiredPermission="ADD_PART" />}>
            <Route path="parts/new" element={<PartCreatePage />} />
          </Route>
          <Route element={<PermissionRoute requiredPermission="UPDATE_PART" />}>
            <Route path="parts/:id/edit" element={<PartEditPage />} />
          </Route>
          <Route element={<PermissionRoute requiredPermission="VIEW_PART" />}>
            <Route path="parts" element={<PartsPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Route>
    </Routes>
  );
};
