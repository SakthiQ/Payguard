import api from './axios';

export interface NetWorthData {
  totalAssets: number;
  totalLiabilities: number;
  netWorth: number;
  currency: string;
}

export const netWorthApi = {
  getNetWorth: async (): Promise<NetWorthData> => {
    const res = await api.get<NetWorthData>('/api/v1/net-worth');
    return res.data;
  },
};
