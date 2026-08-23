import api from './axios';

export interface SubscriptionItem {
  id: number;
  serviceName: string;
  amount: number;
  billingCycle: 'MONTHLY' | 'YEARLY';
  nextRenewalDate: string;
  daysUntilRenewal: number;
  status: 'ACTIVE' | 'CANCEL_REQUESTED' | 'CANCELLED';
  cancellationNotes?: string;
}

export const subscriptionApi = {
  addSubscription: async (
    serviceName: string,
    amount: number,
    nextRenewalDate: string,
    billingCycle = 'MONTHLY'
  ): Promise<SubscriptionItem> => {
    const res = await api.post<SubscriptionItem>('/api/v1/subscriptions', null, {
      params: { serviceName, amount, nextRenewalDate, billingCycle },
    });
    return res.data;
  },

  getSubscriptions: async (): Promise<SubscriptionItem[]> => {
    const res = await api.get<SubscriptionItem[]>('/api/v1/subscriptions');
    return res.data;
  },

  requestCancellation: async (id: number, notes?: string): Promise<SubscriptionItem> => {
    const res = await api.post<SubscriptionItem>(`/api/v1/subscriptions/${id}/cancel`, null, {
      params: { notes },
    });
    return res.data;
  },
};
