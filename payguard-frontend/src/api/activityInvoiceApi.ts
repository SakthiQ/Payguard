import api from './axios';

export interface LineItem {
  id?: number;
  type: 'INCOME' | 'EXPENSE';
  description: string;
  amount: number;
  category?: string;
}

export interface ActivityInvoiceItem {
  id: number;
  userId: number;
  userEmail: string;
  activityTitle: string;
  clientName?: string;
  totalIncome: number;
  totalExpenses: number;
  netProfit: number;
  profitMarginPercent: number;
  status: 'DRAFT' | 'FINALIZED' | 'PAID';
  lineItems: LineItem[];
  createdAt: string;
}

export interface CreateActivityInvoicePayload {
  activityTitle: string;
  clientName?: string;
  status?: string;
  lineItems: LineItem[];
}

export const activityInvoiceApi = {
  createInvoice: async (payload: CreateActivityInvoicePayload): Promise<ActivityInvoiceItem> => {
    const res = await api.post<ActivityInvoiceItem>('/api/v1/activity-invoices', payload);
    return res.data;
  },

  getInvoices: async (): Promise<ActivityInvoiceItem[]> => {
    const res = await api.get<ActivityInvoiceItem[]>('/api/v1/activity-invoices');
    return res.data;
  },

  getInvoiceById: async (id: number): Promise<ActivityInvoiceItem> => {
    const res = await api.get<ActivityInvoiceItem>(`/api/v1/activity-invoices/${id}`);
    return res.data;
  },

  exportCsv: async (id: number, title: string): Promise<void> => {
    const res = await api.get(`/api/v1/activity-invoices/${id}/export/csv`, {
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Activity_Invoice_${title.replace(/\s+/g, '_')}_${id}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  },
};
