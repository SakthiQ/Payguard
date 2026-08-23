import api from './axios';

export interface FraudFlagItem {
  flagId: number;
  transactionId: number;
  transactionReference: string;
  senderAccountNumber: string;
  recipientAccountNumber: string;
  amount: number;
  triggeredRuleCode: string;
  riskScore: number;
  flagReason: string;
  aiRiskInsight?: string;
  status: string; // UNDER_REVIEW, APPROVED, DECLINED
  reviewerEmail?: string;
  reviewNotes?: string;
  reviewedAt?: string;
  createdAt: string;
}

export interface ReviewFraudPayload {
  action: 'APPROVE' | 'DECLINE';
  notes: string;
}

export const fraudApi = {
  getPendingFlags: async (): Promise<FraudFlagItem[]> => {
    const res = await api.get<FraudFlagItem[]>('/api/v1/fraud/flags');
    return res.data;
  },

  getFlagById: async (id: number): Promise<FraudFlagItem> => {
    const res = await api.get<FraudFlagItem>(`/api/v1/fraud/flags/${id}`);
    return res.data;
  },

  reviewFlag: async (id: number, payload: ReviewFraudPayload): Promise<FraudFlagItem> => {
    const res = await api.put<FraudFlagItem>(`/api/v1/fraud/flags/${id}/review`, payload);
    return res.data;
  },
};
