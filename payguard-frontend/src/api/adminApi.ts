import api from './axios';

export interface AdminUserItem {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  userStatus: string;
  walletId?: number;
  accountNumber?: string;
  balance?: number;
  walletStatus?: string;
  createdAt: string;
}

export interface FraudRuleItem {
  id: number;
  ruleCode: string;
  ruleName: string;
  description: string;
  thresholdValue: number;
  timeWindowSeconds?: number;
  enabled: boolean;
  createdAt: string;
}

export interface UpdateRulePayload {
  thresholdValue: number;
  timeWindowSeconds?: number;
  enabled: boolean;
}

export interface AuditLogDocument {
  id: string;
  eventId: string;
  eventType: string;
  actorUserId?: number;
  actorEmail?: string;
  actorRole?: string;
  resourceType: string;
  resourceId?: string;
  payload: Record<string, any>;
  timestamp: string;
}

export const adminApi = {
  getAllUsers: async (): Promise<AdminUserItem[]> => {
    const res = await api.get<AdminUserItem[]>('/api/v1/admin/users');
    return res.data;
  },

  freezeWallet: async (walletId: number): Promise<AdminUserItem> => {
    const res = await api.put<AdminUserItem>(`/api/v1/admin/wallets/${walletId}/freeze`);
    return res.data;
  },

  unfreezeWallet: async (walletId: number): Promise<AdminUserItem> => {
    const res = await api.put<AdminUserItem>(`/api/v1/admin/wallets/${walletId}/unfreeze`);
    return res.data;
  },

  getAllFraudRules: async (): Promise<FraudRuleItem[]> => {
    const res = await api.get<FraudRuleItem[]>('/api/v1/admin/fraud-rules');
    return res.data;
  },

  updateFraudRule: async (ruleCode: string, payload: UpdateRulePayload): Promise<FraudRuleItem> => {
    const res = await api.put<FraudRuleItem>(`/api/v1/admin/fraud-rules/${ruleCode}`, payload);
    return res.data;
  },

  getAllAuditLogs: async (): Promise<AuditLogDocument[]> => {
    const res = await api.get<AuditLogDocument[]>('/api/v1/audit/logs');
    return res.data;
  },
};
