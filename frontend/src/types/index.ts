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
