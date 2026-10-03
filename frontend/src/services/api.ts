import axios from 'axios';

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
