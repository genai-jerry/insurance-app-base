import apiClient from './client';
import {
  Lead,
  CreateLeadRequest,
  UpdateLeadRequest,
  PageResponse,
  LeadStatus,
} from '../types';

export const leadsApi = {
  getAll: async (params?: {
    page?: number;
    size?: number;
    status?: LeadStatus;
    search?: string;
  }): Promise<PageResponse<Lead>> => {
    const response = await apiClient.get<PageResponse<Lead>>('/leads', {
      params,
    });
    return response.data;
  },

  getById: async (id: number): Promise<Lead> => {
    const response = await apiClient.get<Lead>(`/leads/${id}`);
    return response.data;
  },

  create: async (data: CreateLeadRequest): Promise<Lead> => {
    const response = await apiClient.post<Lead>('/leads', data);
    return response.data;
  },

  update: async (id: number, data: UpdateLeadRequest): Promise<Lead> => {
    const response = await apiClient.put<Lead>(`/leads/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/leads/${id}`);
  },

  getMyLeads: async (params?: {
    page?: number;
    size?: number;
    status?: LeadStatus;
  }): Promise<PageResponse<Lead>> => {
    const response = await apiClient.get<PageResponse<Lead>>('/leads', {
      params,
    });
    return response.data;
  },

  updateStatus: async (id: number, status: LeadStatus): Promise<Lead> => {
    const response = await apiClient.put<Lead>(`/leads/${id}/status`, null, {
      params: { status },
    });
    return response.data;
  },
};
