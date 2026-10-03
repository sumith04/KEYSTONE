import axios from 'axios';
import { ApiErrorBody } from '../types';

export const getApiError = (error: unknown): ApiErrorBody => {
  if (axios.isAxiosError(error) && error.response?.data) {
    const data = error.response.data as ApiErrorBody;
    return {
      message: data.message || 'Request failed',
      validationErrors: data.validationErrors,
      status: data.status ?? error.response.status,
    };
  }

  return {
    message: error instanceof Error ? error.message : 'An unexpected error occurred',
  };
};
