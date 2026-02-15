import apiClient from './client';
import {
  Prospectus,
  GenerateProspectusRequest,
} from '../types';

export const prospectusApi = {
  getById: async (id: number): Promise<Prospectus> => {
    const response = await apiClient.get<Prospectus>(
      `/prospectus/${id}`
    );
    return response.data;
  },

  generate: async (
    data: GenerateProspectusRequest
  ): Promise<Prospectus> => {
    const response = await apiClient.post<Prospectus>(
      '/prospectus/generate',
      data
    );
    return response.data;
  },

  download: async (id: number): Promise<Blob> => {
    const response = await apiClient.get(
      `/prospectus/${id}/download`,
      {
        responseType: 'blob',
      }
    );
    return response.data;
  },

  getByLead: async (leadId: number): Promise<Prospectus[]> => {
    const response = await apiClient.get<Prospectus[]>(
      `/prospectus/lead/${leadId}`
    );
    return response.data;
  },
};
