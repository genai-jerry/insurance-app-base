import apiClient from './client';
import { EmailLog, SendEmailRequest, PageResponse } from '../types';

export const emailApi = {
  getAll: async (params?: {
    page?: number;
    size?: number;
  }): Promise<PageResponse<EmailLog>> => {
    const response = await apiClient.get<PageResponse<EmailLog>>(
      '/email',
      { params }
    );
    return response.data;
  },

  getById: async (id: number): Promise<EmailLog> => {
    const response = await apiClient.get<EmailLog>(`/email/${id}`);
    return response.data;
  },

  send: async (data: SendEmailRequest): Promise<EmailLog> => {
    const response = await apiClient.post<EmailLog>('/email/send', data);
    return response.data;
  },

  getByLead: async (leadId: number): Promise<EmailLog[]> => {
    const response = await apiClient.get<EmailLog[]>(
      `/email/lead/${leadId}`
    );
    return response.data;
  },
};
