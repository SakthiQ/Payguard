import api from './axios';

export interface BudgetProgress {
  id: number;
  category: string;
  monthlyCap: number;
  spentAmount: number;
  rolloverAmount: number;
  remainingAmount: number;
  percentUsed: number;
  statusPill: 'OK' | 'WARNING_80' | 'OVERSPENT_100';
}

export const budgetApi = {
  setBudget: async (category: string, monthlyCap: number): Promise<void> => {
    await api.post('/api/v1/budgets', null, {
      params: { category, monthlyCap },
    });
  },

  getBudgetProgress: async (): Promise<BudgetProgress[]> => {
    const res = await api.get<BudgetProgress[]>('/api/v1/budgets/progress');
    return res.data;
  },
};
