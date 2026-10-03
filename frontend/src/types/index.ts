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

export type WorkOrderStatus =
  | 'NEW'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CLOSED'
  | 'CANCELLED';

export type WorkOrderPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type WorkType =
  | 'PREVENTIVE_MAINTENANCE'
  | 'CORRECTIVE_MAINTENANCE'
  | 'INSPECTION'
  | 'EMERGENCY'
  | 'INSTALLATION'
  | 'OTHER';

export interface WorkOrder {
  id: number;
  workOrderNumber: string;
  title: string;
  description: string | null;
  customerId: number;
  customerCode: string;
  customerName: string;
  siteId: number;
  siteCode: string;
  siteName: string;
  assignedTechnicianId: number | null;
  assignedTechnicianName: string | null;
  assignedTechnicianEmail: string | null;
  status: WorkOrderStatus;
  priority: WorkOrderPriority;
  workType: WorkType;
  scheduledStart: string | null;
  scheduledEnd: string | null;
  actualStart: string | null;
  actualEnd: string | null;
  notes: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WorkOrderPageResponse {
  content: WorkOrder[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface WorkOrderSummary {
  assigned: number;
  inProgress: number;
  onHold: number;
  completed: number;
}

export interface CreateWorkOrderRequest {
  title: string;
  description?: string;
  customerId: number;
  siteId: number;
  priority: WorkOrderPriority;
  workType: WorkType;
  scheduledStart?: string;
  scheduledEnd?: string;
  notes?: string;
}

export interface UpdateWorkOrderRequest extends CreateWorkOrderRequest {}

export interface AssignWorkOrderRequest {
  technicianId: number;
}

export interface WorkOrderListQueryParams extends ListQueryParams {
  priority?: WorkOrderPriority | string;
  siteId?: number;
  technicianId?: number;
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

export type PartStatus = 'ACTIVE' | 'INACTIVE';

export interface Part {
  id: number;
  partNumber: string;
  name: string;
  description: string | null;
  category: string | null;
  unitOfMeasure: string | null;
  unitCost: number | string;
  quantityInStock: number;
  reorderLevel: number;
  status: PartStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PartPageResponse {
  content: Part[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreatePartRequest {
  partNumber: string;
  name: string;
  description?: string;
  category?: string;
  unitOfMeasure?: string;
  unitCost: number;
  quantityInStock: number;
  reorderLevel: number;
  status?: PartStatus;
}

export interface UpdatePartRequest extends CreatePartRequest {
  status: PartStatus;
}

export interface PartListQueryParams extends ListQueryParams {
  category?: string;
}

export interface WorkOrderPart {
  id: number;
  workOrderId: number;
  partId: number;
  partNumber: string;
  partName: string;
  quantityUsed: number;
  unitCostAtUsage: number | string;
  totalCost: number | string;
  usedById: number | null;
  usedByName: string | null;
  usedAt: string;
}

export interface CreateWorkOrderPartRequest {
  partId: number;
  quantity: number;
}

export interface UpdateWorkOrderPartRequest {
  quantity: number;
}

export interface TimeLog {
  id: number;
  workOrderId: number;
  technicianId: number;
  technicianName: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTimeLogRequest {
  startTime: string;
  endTime: string;
  notes?: string;
}

export interface UpdateTimeLogRequest extends CreateTimeLogRequest {}
