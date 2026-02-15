// User and Auth Types
export interface User {
  id: number;
  name: string;
  email: string;
  role: 'AGENT' | 'ADMIN';
  createdAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  id: number;
  name: string;
  email: string;
  role: 'AGENT' | 'ADMIN';
}

export interface RegisterRequest {
  name: string;
  password: string;
  email: string;
  role: 'AGENT' | 'ADMIN';
}

// Lead Types
export interface Lead {
  id: number;
  name: string;
  email: string;
  phone: string;
  location?: string;
  age?: number;
  incomeBand?: string;
  leadSource?: string;
  status: LeadStatus;
  assignedAgentId?: number;
  assignedAgentName?: string;
  preferredTimeWindows?: Record<string, unknown>;
  timezone?: string;
  consentFlags?: Record<string, unknown>;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export type LeadStatus =
  | 'NEW'
  | 'CONTACTED'
  | 'QUALIFIED'
  | 'PROPOSAL_SENT'
  | 'CONVERTED'
  | 'LOST';

export interface CreateLeadRequest {
  name: string;
  email: string;
  phone: string;
  leadSource?: string;
  notes?: string;
  assignedAgentId?: number;
  location?: string;
  age?: number;
}

export interface UpdateLeadRequest {
  name?: string;
  email?: string;
  phone?: string;
  status?: LeadStatus;
  notes?: string;
  assignedAgentId?: number;
  location?: string;
}

// Product Types
export interface Product {
  id: number;
  name: string;
  categoryId: number;
  categoryName?: string;
  insurer?: string;
  planType?: string;
  detailsJson?: Record<string, unknown>;
  eligibilityJson?: Record<string, unknown>;
  tags?: string[];
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: number;
  name: string;
  description: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateProductRequest {
  categoryId: number;
  name: string;
  insurer: string;
  planType: string;
  detailsJson?: Record<string, unknown>;
  eligibilityJson?: Record<string, unknown>;
  tags?: string[];
}

export interface UpdateProductRequest {
  name?: string;
  categoryId?: number;
  insurer?: string;
  planType?: string;
  detailsJson?: Record<string, unknown>;
  eligibilityJson?: Record<string, unknown>;
  tags?: string[];
}

// Voice Session Types
export interface VoiceSession {
  id: number;
  leadId: number;
  leadName?: string;
  agentId: number;
  agentName?: string;
  callTaskId?: number;
  sessionId?: string;
  startedAt?: string;
  endedAt?: string;
  durationSeconds?: number;
  transcriptText?: string;
  extractedNeedsJson?: Record<string, unknown>;
  recommendationsJson?: Record<string, unknown>;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  errorMessage?: string;
  createdAt: string;
}

export interface StartVoiceSessionRequest {
  leadId: number;
}

// Scheduler Types
export interface CallTask {
  id: number;
  leadId: number;
  leadName?: string;
  leadPhone?: string;
  agentId: number;
  agentName?: string;
  scheduledTime: string;
  status: 'PENDING' | 'DONE' | 'MISSED' | 'CANCELLED';
  outcome?: string;
  notes?: string;
  completedAt?: string;
  createdAt: string;
}

export interface ScheduleCallRequest {
  leadId: number;
  agentId: number;
  scheduledTime: string;
  notes?: string;
  usePreferredTimeWindow?: boolean;
}

export interface UpdateCallTaskRequest {
  scheduledTime?: string;
  status?: 'PENDING' | 'DONE' | 'CANCELLED' | 'MISSED';
  outcome?: string;
  notes?: string;
}

// Prospectus Types
export interface Prospectus {
  id: number;
  leadId: number;
  leadName?: string;
  agentId: number;
  agentName?: string;
  voiceSessionId?: number;
  version?: number;
  htmlContent?: string;
  pdfPath?: string;
  pdfUrl?: string;
  createdAt: string;
}

export interface GenerateProspectusRequest {
  leadId: number;
  voiceSessionId?: number;
}

// Email Types
export interface EmailLog {
  id: number;
  leadId: number;
  leadName?: string;
  agentId: number;
  agentName?: string;
  prospectusId?: number;
  toEmail: string;
  subject: string;
  body: string;
  status: 'SENT' | 'FAILED' | 'PENDING';
  providerMessageId?: string;
  errorMessage?: string;
  sentAt?: string;
  createdAt: string;
}

export interface SendEmailRequest {
  leadId: number;
  agentId: number;
  toEmail: string;
  subject: string;
  body: string;
  prospectusId?: number;
  attachProspectus?: boolean;
}

// Admin Types
export interface AdminSetting {
  id: number;
  key: string;
  value: string;
  description?: string;
  updatedByUserId?: number;
  updatedByUserName?: string;
  updatedAt: string;
  createdAt: string;
}

export interface UpdateAdminSettingRequest {
  value: string;
}

export interface AuditLog {
  id: number;
  actorId?: number;
  actorName?: string;
  action: string;
  entity?: string;
  entityId?: number;
  beforeJson?: Record<string, unknown>;
  afterJson?: Record<string, unknown>;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}

// Document Types
export interface ProductDocument {
  id: number;
  productId: number;
  filename: string;
  storagePath?: string;
  storageUrl?: string;
  fileSize?: number;
  contentType?: string;
  extractedText?: string;
  createdAt: string;
}

// Dashboard Stats
export interface DashboardStats {
  totalLeads: number;
  newLeads: number;
  callsToday: number;
  callsPending: number;
  conversionRate: number;
  leadsByStatus: Record<LeadStatus, number>;
}

// Pagination
export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// API Error
export interface ApiError {
  message: string;
  status: number;
  timestamp: string;
  path?: string;
}
