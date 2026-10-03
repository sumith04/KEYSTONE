/**
 * KEYSTONE Foundational TypeScript Definitions
 */

export interface SystemStatus {
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  application: string;
  timestamp: string;
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp?: string;
}

export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

export type CustomerStatus = 'ACTIVE' | 'INACTIVE';
export type SiteStatus = 'ACTIVE' | 'INACTIVE';

export interface Customer {
  id: number;
  customerCode: string;
  companyName: string;
  contactFirstName: string | null;
  contactLastName: string | null;
  email: string | null;
  phone: string | null;
  alternatePhone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  status: CustomerStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerPageResponse {
  content: Customer[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateCustomerRequest {
  customerCode: string;
  companyName: string;
  contactFirstName?: string;
  contactLastName?: string;
  email?: string;
  phone?: string;
  alternatePhone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  notes?: string;
}

export interface UpdateCustomerRequest extends CreateCustomerRequest {}

export interface Site {
  id: number;
  siteCode: string;
  siteName: string;
  customerId: number;
  customerCode: string;
  customerName: string;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  contactName: string | null;
  contactPhone: string | null;
  contactEmail: string | null;
  status: SiteStatus;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SitePageResponse {
  content: Site[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateSiteRequest {
  siteCode: string;
  siteName: string;
  customerId: number;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  description?: string;
}

export interface UpdateSiteRequest extends CreateSiteRequest {}

export interface ListQueryParams {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
  sort?: string;
  customerId?: number;
}

export type Role = 'ADMIN' | 'MANAGER' | 'DISPATCHER' | 'TECHNICIAN' | 'CUSTOMER';

export interface AuthUser {
  id: number;
  firstName: string;
  lastName: string;
  userEmail: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface LoginRequest {
  userEmail: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  message: string;
  user: AuthUser;
}

export interface MessageResponse {
  message: string;
  timestamp?: string;
}
