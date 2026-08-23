import api from './axios';

export interface TransferRequest {
  recipientAccountNumber: string;
  amount: number;
  description?: string;
}

export interface TransferResponse {
  transactionReference: string;
  senderAccountNumber: string;
  recipientAccountNumber: string;
  amount: number;
  status: string; // COMPLETED, FLAGGED, DECLINED
  message: string;
  createdAt: string;
}

export interface TransactionItem {
  id: number;
  transactionReference: string;
  senderAccountNumber: string;
  recipientAccountNumber: string;
  amount: number;
  type: string;
  status: string;
  description?: string;
  createdAt: string;
}

export const txApi = {
  processTransfer: async (payload: TransferRequest, idempotencyKey: string): Promise<TransferResponse> => {
    const res = await api.post<TransferResponse>('/api/v1/transactions', payload, {
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
    });
    return res.data;
  },

  getHistory: async (): Promise<TransactionItem[]> => {
    const res = await api.get<TransactionItem[]>('/api/v1/transactions');
    return res.data;
  },

  getById: async (id: number): Promise<TransactionItem> => {
    const res = await api.get<TransactionItem>(`/api/v1/transactions/${id}`);
    return res.data;
  },

  downloadCsvStatement: async (): Promise<void> => {
    const res = await api.get('/api/v1/transactions/export/csv', {
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `PayGuard_Statement_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  },
};
