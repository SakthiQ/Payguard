import api from './axios';

export interface ExpenseItem {
  id: number;
  userId: number;
  userEmail: string;
  accountType: 'WALLET' | 'BANK' | 'CASH' | 'CREDIT';
  category: 'FOOD' | 'RENT' | 'BILLS' | 'ENTERTAINMENT' | 'TRAVEL' | 'BUSINESS' | 'SUBSCRIPTION' | 'OTHER';
  amount: number;
  currency: string;
  merchantName: string;
  description?: string;
  tags?: string;
  taxDeductible: boolean;
  recurring: boolean;
  activityInvoiceId?: number;
  createdAt: string;
}

export interface ExpenseSummary {
  totalExpenses: number;
  totalTaxDeductible: number;
  categoryBreakdown: Record<string, number>;
  accountTypeBreakdown: Record<string, number>;
}

export interface LogExpensePayload {
  category: string;
  accountType: string;
  amount: number;
  currency?: string;
  merchantName?: string;
  description?: string;
  tags?: string;
  taxDeductible?: boolean;
  recurring?: boolean;
  activityInvoiceId?: number;
}

export const expenseApi = {
  logExpense: async (payload: LogExpensePayload): Promise<ExpenseItem> => {
    const res = await api.post<ExpenseItem>('/api/v1/expenses', payload);
    return res.data;
  },

  getExpenses: async (params?: {
    category?: string;
    accountType?: string;
    taxDeductible?: boolean;
    query?: string;
  }): Promise<ExpenseItem[]> => {
    const res = await api.get<ExpenseItem[]>('/api/v1/expenses', { params });
    return res.data;
  },

  getSummary: async (): Promise<ExpenseSummary> => {
    const res = await api.get<ExpenseSummary>('/api/v1/expenses/summary');
    return res.data;
  },

  deleteExpense: async (id: number): Promise<void> => {
    await api.delete(`/api/v1/expenses/${id}`);
  },

  getAiInsights: async (): Promise<string[]> => {
    const res = await api.get<string[]>('/api/v1/expense-analytics/insights');
    return res.data;
  },

  scanReceipt: async (filename: string): Promise<LogExpensePayload> => {
    const res = await api.post<LogExpensePayload>('/api/v1/expense-analytics/scan-receipt', null, {
      params: { filename },
    });
    return res.data;
  },
};
