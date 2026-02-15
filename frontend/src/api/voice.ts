import apiClient from './client';
import {
  VoiceSession,
  StartVoiceSessionRequest,
} from '../types';

export const voiceApi = {
  startSession: async (
    data: StartVoiceSessionRequest
  ): Promise<VoiceSession> => {
    const response = await apiClient.post<VoiceSession>(
      '/voice/sessions/start',
      data
    );
    return response.data;
  },

  getSession: async (sessionId: number): Promise<VoiceSession> => {
    const response = await apiClient.get<VoiceSession>(
      `/voice/sessions/${sessionId}`
    );
    return response.data;
  },

  stopSession: async (sessionId: number): Promise<VoiceSession> => {
    const response = await apiClient.post<VoiceSession>(
      `/voice/sessions/${sessionId}/stop`
    );
    return response.data;
  },

  getNeeds: async (sessionId: number): Promise<Record<string, unknown>> => {
    const response = await apiClient.get<Record<string, unknown>>(
      `/voice/sessions/${sessionId}/needs`
    );
    return response.data;
  },

  getRecommendations: async (
    sessionId: number
  ): Promise<Record<string, unknown>> => {
    const response = await apiClient.get<Record<string, unknown>>(
      `/voice/sessions/${sessionId}/recommendations`
    );
    return response.data;
  },

  getLeadSessions: async (leadId: number): Promise<VoiceSession[]> => {
    const response = await apiClient.get<VoiceSession[]>(
      `/voice/sessions/lead/${leadId}`
    );
    return response.data;
  },
};
