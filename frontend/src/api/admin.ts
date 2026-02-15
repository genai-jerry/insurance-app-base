import apiClient from './client';
import {
  User,
  AdminSetting,
  UpdateAdminSettingRequest,
  AuditLog,
  PageResponse,
  DashboardStats,
} from '../types';

export const adminApi = {
  // User Management
  getAllUsers: async (params?: {
    page?: number;
    size?: number;
  }): Promise<User[]> => {
    const response = await apiClient.get<User[]>('/admin/users', {
      params,
    });
    return response.data;
  },

  getUserById: async (id: number): Promise<User> => {
    const response = await apiClient.get<User>(`/admin/users/${id}`);
    return response.data;
  },

  updateUser: async (
    id: number,
    data: { name?: string; email?: string; role?: string }
  ): Promise<User> => {
    const response = await apiClient.put<User>(`/admin/users/${id}`, data);
    return response.data;
  },

  deleteUser: async (id: number): Promise<void> => {
    await apiClient.delete(`/admin/users/${id}`);
  },

  // Settings (formerly Config)
  getAllConfigs: async (): Promise<AdminSetting[]> => {
    const response = await apiClient.get<AdminSetting[]>('/admin/settings');
    return response.data;
  },

  getConfig: async (key: string): Promise<AdminSetting> => {
    const response = await apiClient.get<AdminSetting>(`/admin/settings/${key}`);
    return response.data;
  },

  updateConfig: async (
    key: string,
    data: UpdateAdminSettingRequest
  ): Promise<AdminSetting> => {
    const response = await apiClient.put<AdminSetting>(
      `/admin/settings/${key}`,
      data
    );
    return response.data;
  },

  // Audit Logs
  getAuditLogs: async (params?: {
    page?: number;
    size?: number;
    userId?: number;
    action?: string;
    entity?: string;
  }): Promise<PageResponse<AuditLog>> => {
    const response = await apiClient.get<PageResponse<AuditLog>>(
      '/admin/audit',
      { params }
    );
    return response.data;
  },

  // Dashboard Stats
  getDashboardStats: async (): Promise<DashboardStats> => {
    const response = await apiClient.get<DashboardStats>('/admin/stats');
    return response.data;
  },
};
