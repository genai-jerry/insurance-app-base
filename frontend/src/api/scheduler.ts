import apiClient from './client';
import {
  CallTask,
  ScheduleCallRequest,
} from '../types';

export const schedulerApi = {
  getAll: async (): Promise<CallTask[]> => {
    const response = await apiClient.get<CallTask[]>(
      '/scheduler/tasks/pending'
    );
    return response.data;
  },

  getById: async (id: number): Promise<CallTask> => {
    const response = await apiClient.get<CallTask>(`/scheduler/tasks/${id}`);
    return response.data;
  },

  schedule: async (data: ScheduleCallRequest): Promise<CallTask> => {
    const response = await apiClient.post<CallTask>('/scheduler/tasks', data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/scheduler/tasks/${id}`);
  },

  getToday: async (): Promise<CallTask[]> => {
    const response = await apiClient.get<CallTask[]>(
      '/scheduler/tasks/today'
    );
    return response.data;
  },

  complete: async (id: number, notes?: string): Promise<CallTask> => {
    const response = await apiClient.post<CallTask>(
      `/scheduler/tasks/${id}/complete`,
      null,
      { params: { notes } }
    );
    return response.data;
  },
};
