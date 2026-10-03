import axios from 'axios';
import {
  AssignWorkOrderRequest,
  AuthResponse,
  AuthUser,
  CreateCustomerRequest,
  CreateSiteRequest,
  CreateWorkOrderRequest,
  Customer,
  CustomerPageResponse,
  CustomerStatus,
  ListQueryParams,
  LoginRequest,
  MessageResponse,
  Site,
  SitePageResponse,
  SiteStatus,
  UpdateCustomerRequest,
  UpdateSiteRequest,
  UpdateWorkOrderRequest,
  WorkOrder,
  WorkOrderListQueryParams,
  WorkOrderPageResponse,
  WorkOrderSummary,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const AUTH_TOKEN_KEY = 'keystone_token';
export const AUTH_USER_KEY = 'keystone_user';
export const AUTH_UNAUTHORIZED_EVENT = 'keystone:unauthorized';

const isPublicAuthRequest = (url?: string) => {
  if (!url) {
    return false;
  }
  return (
    url.includes('/auth/login') ||
    url.includes('/auth/register') ||
    url.includes('/auth/forgot-password') ||
    url.includes('/auth/reset-password')
  );
};

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request Interceptor: Attach JWT token if available
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle 401 Unauthorized
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401 && !isPublicAuthRequest(error.config?.url)) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      localStorage.removeItem(AUTH_USER_KEY);
      window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT));
    }
    return Promise.reject(error);
  }
);

export interface SystemHealthResponse {
  status: string;
  application: string;
  timestamp: string;
}

export interface UserPermissionsResponse {
  role: string;
  permissions: string[];
}

export const fetchSystemHealth = async (): Promise<SystemHealthResponse> => {
  const response = await apiClient.get<SystemHealthResponse>('/health');
  return response.data;
};

export const fetchUserPermissions = async (): Promise<UserPermissionsResponse> => {
  const response = await apiClient.get<UserPermissionsResponse>('/auth/permissions');
  return response.data;
};

export const login = async (payload: LoginRequest): Promise<AuthResponse> => {
  const response = await apiClient.post<AuthResponse>('/auth/login', payload);
  return response.data;
};

export const logout = async (): Promise<MessageResponse> => {
  const response = await apiClient.post<MessageResponse>('/auth/logout');
  return response.data;
};

export const fetchCurrentUser = async (): Promise<AuthUser> => {
  const response = await apiClient.get<AuthUser>('/auth/me');
  return response.data;
};

const toListParams = (params: ListQueryParams | WorkOrderListQueryParams = {}) => {
  const query: Record<string, string | number> = {
    page: params.page ?? 0,
    size: params.size ?? 10,
    sort: params.sort ?? 'createdAt',
  };

  if (params.search && params.search.trim()) {
    query.search = params.search.trim();
  }
  if (params.status) {
    query.status = params.status;
  }
  if (params.customerId != null) {
    query.customerId = params.customerId;
  }

  const workOrderParams = params as WorkOrderListQueryParams;
  if (workOrderParams.priority) {
    query.priority = workOrderParams.priority;
  }
  if (workOrderParams.siteId != null) {
    query.siteId = workOrderParams.siteId;
  }
  if (workOrderParams.technicianId != null) {
    query.technicianId = workOrderParams.technicianId;
  }

  return query;
};

export const getCustomers = async (params: ListQueryParams = {}): Promise<CustomerPageResponse> => {
  const response = await apiClient.get<CustomerPageResponse>('/customers', { params: toListParams(params) });
  return response.data;
};

export const getCustomer = async (id: number): Promise<Customer> => {
  const response = await apiClient.get<Customer>(`/customers/${id}`);
  return response.data;
};

export const createCustomer = async (payload: CreateCustomerRequest): Promise<Customer> => {
  const response = await apiClient.post<Customer>('/customers', payload);
  return response.data;
};

export const updateCustomer = async (id: number, payload: UpdateCustomerRequest): Promise<Customer> => {
  const response = await apiClient.put<Customer>(`/customers/${id}`, payload);
  return response.data;
};

export const updateCustomerStatus = async (id: number, status: CustomerStatus): Promise<Customer> => {
  const response = await apiClient.patch<Customer>(`/customers/${id}/status`, null, { params: { status } });
  return response.data;
};

export const deleteCustomer = async (id: number): Promise<void> => {
  await apiClient.delete(`/customers/${id}`);
};

export const getSites = async (params: ListQueryParams = {}): Promise<SitePageResponse> => {
  const response = await apiClient.get<SitePageResponse>('/sites', { params: toListParams(params) });
  return response.data;
};

export const getSite = async (id: number): Promise<Site> => {
  const response = await apiClient.get<Site>(`/sites/${id}`);
  return response.data;
};

export const createSite = async (payload: CreateSiteRequest): Promise<Site> => {
  const response = await apiClient.post<Site>('/sites', payload);
  return response.data;
};

export const updateSite = async (id: number, payload: UpdateSiteRequest): Promise<Site> => {
  const response = await apiClient.put<Site>(`/sites/${id}`, payload);
  return response.data;
};

export const updateSiteStatus = async (id: number, status: SiteStatus): Promise<Site> => {
  const response = await apiClient.patch<Site>(`/sites/${id}/status`, null, { params: { status } });
  return response.data;
};

export const deleteSite = async (id: number): Promise<void> => {
  await apiClient.delete(`/sites/${id}`);
};

export const getWorkOrders = async (params: WorkOrderListQueryParams = {}): Promise<WorkOrderPageResponse> => {
  const response = await apiClient.get<WorkOrderPageResponse>('/work-orders', { params: toListParams(params) });
  return response.data;
};

export const getWorkOrderSummary = async (): Promise<WorkOrderSummary> => {
  const response = await apiClient.get<WorkOrderSummary>('/work-orders/summary');
  return response.data;
};

export const getWorkOrder = async (id: number): Promise<WorkOrder> => {
  const response = await apiClient.get<WorkOrder>(`/work-orders/${id}`);
  return response.data;
};

export const getWorkOrderByNumber = async (workOrderNumber: string): Promise<WorkOrder> => {
  const response = await apiClient.get<WorkOrder>(`/work-orders/number/${encodeURIComponent(workOrderNumber)}`);
  return response.data;
};

export const createWorkOrder = async (payload: CreateWorkOrderRequest): Promise<WorkOrder> => {
  const response = await apiClient.post<WorkOrder>('/work-orders', payload);
  return response.data;
};

export const updateWorkOrder = async (id: number, payload: UpdateWorkOrderRequest): Promise<WorkOrder> => {
  const response = await apiClient.put<WorkOrder>(`/work-orders/${id}`, payload);
  return response.data;
};

export const deleteWorkOrder = async (id: number): Promise<void> => {
  await apiClient.delete(`/work-orders/${id}`);
};

export const assignWorkOrder = async (id: number, payload: AssignWorkOrderRequest): Promise<WorkOrder> => {
  const response = await apiClient.post<WorkOrder>(`/work-orders/${id}/assign`, payload);
  return response.data;
};

export const startWorkOrder = async (id: number): Promise<WorkOrder> => {
  const response = await apiClient.post<WorkOrder>(`/work-orders/${id}/start`);
  return response.data;
};

export const holdWorkOrder = async (id: number): Promise<WorkOrder> => {
  const response = await apiClient.post<WorkOrder>(`/work-orders/${id}/hold`);
  return response.data;
};

export const resumeWorkOrder = async (id: number): Promise<WorkOrder> => {
  const response = await apiClient.post<WorkOrder>(`/work-orders/${id}/resume`);
  return response.data;
};

export const completeWorkOrder = async (id: number): Promise<WorkOrder> => {
  const response = await apiClient.post<WorkOrder>(`/work-orders/${id}/complete`);
  return response.data;
};

export const closeWorkOrder = async (id: number): Promise<WorkOrder> => {
  const response = await apiClient.post<WorkOrder>(`/work-orders/${id}/close`);
  return response.data;
};

export const cancelWorkOrder = async (id: number): Promise<WorkOrder> => {
  const response = await apiClient.post<WorkOrder>(`/work-orders/${id}/cancel`);
  return response.data;
};

export const getAssignableTechnicians = async (): Promise<AuthUser[]> => {
  const response = await apiClient.get<AuthUser[]>('/work-orders/technicians');
  return response.data;
};
