import api from './axios';

export interface WalletResponse {
  walletId: number;
  accountNumber: string;
  balance: number;
  currency: string;
  status: string;
  userId: number;
  userEmail: string;
  createdAt: string;
}

export interface DepositResponse {
  walletId: number;
  accountNumber: string;
  newBalance: number;
  transactionReference: string;
  message: string;
}

export const walletApi = {
  getMyWallet: async (): Promise<WalletResponse> => {
    const res = await api.get<WalletResponse>('/api/v1/wallets/me');
    return res.data;
  },

  getWalletById: async (id: number): Promise<WalletResponse> => {
    const res = await api.get<WalletResponse>(`/api/v1/wallets/${id}`);
    return res.data;
  },

  deposit: async (walletId: number, amount: number): Promise<DepositResponse> => {
    const res = await api.post<DepositResponse>(`/api/v1/wallets/${walletId}/deposit`, { amount });
    return res.data;
  },
};
