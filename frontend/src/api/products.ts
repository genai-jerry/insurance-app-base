import apiClient from './client';
import {
  Product,
  Category,
  CreateProductRequest,
  UpdateProductRequest,
} from '../types';

export const productsApi = {
  getAll: async (params?: {
    categoryId?: number;
    size?: number;
  }): Promise<Product[]> => {
    const response = await apiClient.get<Product[]>('/products', {
      params,
    });
    return response.data;
  },

  getById: async (id: number): Promise<Product> => {
    const response = await apiClient.get<Product>(`/products/${id}`);
    return response.data;
  },

  create: async (data: CreateProductRequest): Promise<Product> => {
    const response = await apiClient.post<Product>('/products', data);
    return response.data;
  },

  update: async (id: number, data: UpdateProductRequest): Promise<Product> => {
    const response = await apiClient.put<Product>(`/products/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/products/${id}`);
  },

  getByCategory: async (categoryId: number): Promise<Product[]> => {
    const response = await apiClient.get<Product[]>(
      `/products/category/${categoryId}`
    );
    return response.data;
  },
};

export const categoriesApi = {
  getAll: async (): Promise<Category[]> => {
    const response = await apiClient.get<Category[]>('/products/categories');
    return response.data;
  },

  getById: async (id: number): Promise<Category> => {
    const response = await apiClient.get<Category>(
      `/products/categories/${id}`
    );
    return response.data;
  },

  create: async (data: {
    name: string;
    description: string;
  }): Promise<Category> => {
    const response = await apiClient.post<Category>(
      '/products/categories',
      data
    );
    return response.data;
  },

  update: async (
    id: number,
    data: { name?: string; description?: string }
  ): Promise<Category> => {
    const response = await apiClient.put<Category>(
      `/products/categories/${id}`,
      data
    );
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/products/categories/${id}`);
  },
};
