import axios from 'axios';
import {
  CreateCustomerRequest,
  CreateSiteRequest,
  Customer,
  CustomerPageResponse,
  CustomerStatus,
  ListQueryParams,
  Site,
  SitePageResponse,
  SiteStatus,
  UpdateCustomerRequest,
  UpdateSiteRequest,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

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
    const token = localStorage.getItem('keystone_token');
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
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('keystone_token');
      localStorage.removeItem('keystone_user');
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

const toListParams = (params: ListQueryParams = {}) => {
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
