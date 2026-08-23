import React, { useEffect, useState } from 'react';
import { expenseApi } from '../api/expenseApi';
import type { ExpenseItem, ExpenseSummary } from '../api/expenseApi';
import { activityInvoiceApi } from '../api/activityInvoiceApi';
import type { ActivityInvoiceItem, LineItem } from '../api/activityInvoiceApi';
import { budgetApi } from '../api/budgetApi';
import type { BudgetProgress } from '../api/budgetApi';
import { subscriptionApi } from '../api/subscriptionApi';
import type { SubscriptionItem } from '../api/subscriptionApi';
import { netWorthApi } from '../api/netWorthApi';
import type { NetWorthData } from '../api/netWorthApi';
import {
  PieChart,
  Receipt,
  FileText,
  PlusCircle,
  TrendingUp,
  Trash2,
  Download,
  Search,
  Sparkles,
  Repeat
} from 'lucide-react';

export const ExpenseTrackerPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'overview' | 'invoices' | 'budgets' | 'subscriptions' | 'tax'>('overview');

  // Overview State
  const [expenses, setExpenses] = useState<ExpenseItem[]>([]);
  const [summary, setSummary] = useState<ExpenseSummary | null>(null);
  const [netWorth, setNetWorth] = useState<NetWorthData | null>(null);
  const [aiInsights, setAiInsights] = useState<string[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  // New Expense Form State
  const [category, setCategory] = useState('FOOD');
  const [accountType, setAccountType] = useState('CASH');
  const [amount, setAmount] = useState('');
  const [merchantName, setMerchantName] = useState('');
  const [description, setDescription] = useState('');
  const [tags, setTags] = useState('');
  const [taxDeductible, setTaxDeductible] = useState(false);
  const [recurring, setRecurring] = useState(false);
  const [scanFilename, setScanFilename] = useState('');

  // Activity Invoice State
  const [invoices, setInvoices] = useState<ActivityInvoiceItem[]>([]);
  const [activityTitle, setActivityTitle] = useState('');
  const [clientName, setClientName] = useState('');
  const [lineItems, setLineItems] = useState<LineItem[]>([
    { type: 'INCOME', description: 'Client Consulting Fee', amount: 3000, category: 'SERVICES' },
    { type: 'EXPENSE', description: 'Software License & Tools', amount: 450, category: 'SOFTWARE' },
  ]);

  // Budget State
  const [budgets, setBudgets] = useState<BudgetProgress[]>([]);
  const [budgetCategory, setBudgetCategory] = useState('FOOD');
  const [budgetCap, setBudgetCap] = useState('');

  // Subscription State
  const [subscriptions, setSubscriptions] = useState<SubscriptionItem[]>([]);
  const [subServiceName, setSubServiceName] = useState('');
  const [subAmount, setSubAmount] = useState('');
  const [subRenewalDate, setSubRenewalDate] = useState('');

  useEffect(() => {
    loadAllData();
  }, []);

  const loadAllData = async () => {
    try {
      const [expData, sumData, nwData, insightsData, invData, budData, subData] = await Promise.all([
        expenseApi.getExpenses(),
        expenseApi.getSummary(),
        netWorthApi.getNetWorth(),
        expenseApi.getAiInsights(),
        activityInvoiceApi.getInvoices(),
        budgetApi.getBudgetProgress(),
        subscriptionApi.getSubscriptions(),
      ]);
      setExpenses(expData);
      setSummary(sumData);
      setNetWorth(nwData);
      setAiInsights(insightsData);
      setInvoices(invData);
      setBudgets(budData);
      setSubscriptions(subData);
    } catch (e) {
      console.error('Failed to load expense tracker data', e);
    }
  };

  // Handlers
  const handleLogExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!amount || parseFloat(amount) <= 0) return;

    try {
      await expenseApi.logExpense({
        category,
        accountType,
        amount: parseFloat(amount),
        merchantName: merchantName || 'General Merchant',
        description,
        tags,
        taxDeductible,
        recurring,
      });
      setAmount('');
      setMerchantName('');
      setDescription('');
      setTags('');
      setTaxDeductible(false);
      setRecurring(false);
      loadAllData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to log expense');
    }
  };

  const handleScanReceipt = async () => {
    if (!scanFilename) return;
    try {
      const scanned = await expenseApi.scanReceipt(scanFilename);
      setCategory(scanned.category || 'FOOD');
      setAccountType(scanned.accountType || 'CASH');
      setAmount(scanned.amount?.toString() || '');
      setMerchantName(scanned.merchantName || '');
      setDescription(scanned.description || '');
      setTags(scanned.tags || '');
      setTaxDeductible(!!scanned.taxDeductible);
      alert('Receipt scanned! Form pre-filled with extracted data.');
    } catch (e) {
      alert('Receipt scanning failed');
    }
  };

  const handleDeleteExpense = async (id: number) => {
    if (!confirm('Are you sure you want to delete this expense?')) return;
    try {
      await expenseApi.deleteExpense(id);
      loadAllData();
    } catch (e) {
      alert('Failed to delete expense');
    }
  };

  const handleCreateActivityInvoice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activityTitle || lineItems.length === 0) return;

    try {
      await activityInvoiceApi.createInvoice({
        activityTitle,
        clientName,
        status: 'FINALIZED',
        lineItems,
      });
      setActivityTitle('');
      setClientName('');
      loadAllData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create activity invoice');
    }
  };

  const addLineItem = (type: 'INCOME' | 'EXPENSE') => {
    setLineItems([
      ...lineItems,
      { type, description: type === 'INCOME' ? 'New Income Item' : 'New Expense Item', amount: 100, category: 'GENERAL' },
    ]);
  };

  const updateLineItem = (index: number, field: keyof LineItem, val: any) => {
    const updated = [...lineItems];
    updated[index] = { ...updated[index], [field]: val };
    setLineItems(updated);
  };

  const removeLineItem = (index: number) => {
    setLineItems(lineItems.filter((_, i) => i !== index));
  };

  const handleSetBudget = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!budgetCap || parseFloat(budgetCap) <= 0) return;
    try {
      await budgetApi.setBudget(budgetCategory, parseFloat(budgetCap));
      setBudgetCap('');
      loadAllData();
    } catch (e) {
      alert('Failed to set budget');
    }
  };

  const handleAddSubscription = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subServiceName || !subAmount || !subRenewalDate) return;
    try {
      await subscriptionApi.addSubscription(subServiceName, parseFloat(subAmount), subRenewalDate);
      setSubServiceName('');
      setSubAmount('');
      setSubRenewalDate('');
      loadAllData();
    } catch (e) {
      alert('Failed to add subscription');
    }
  };

  const handleCancelSubscription = async (id: number) => {
    try {
      await subscriptionApi.requestCancellation(id, 'User initiated 1-click cancel request');
      loadAllData();
    } catch (e) {
      alert('Failed to request cancellation');
    }
  };

  // Filtered Expenses
  const filteredExpenses = expenses.filter(
    (e) =>
      e.merchantName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.category?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.tags?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Invoice Totals Preview
  const previewIncome = lineItems.filter((i) => i.type === 'INCOME').reduce((acc, i) => acc + (Number(i.amount) || 0), 0);
  const previewExpense = lineItems.filter((i) => i.type === 'EXPENSE').reduce((acc, i) => acc + (Number(i.amount) || 0), 0);
  const previewNet = previewIncome - previewExpense;
  const previewMargin = previewIncome > 0 ? ((previewNet / previewIncome) * 100).toFixed(1) : '0.0';

  return (
    <div className="container" style={{ paddingBottom: '4rem' }}>
      {/* Top Header & Net Worth Widget */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, background: 'linear-gradient(135deg, #A855F7, #3B82F6)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            Personal Expense Tracker & Activity Ledger
          </h1>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Multi-account expense logging, single-ledger activity invoices, subscription hub & AI spending insights
          </p>
        </div>

        {/* Net Worth Card */}
        {netWorth && (
          <div className="glass-card" style={{ padding: '1rem 1.5rem', display: 'flex', alignItems: 'center', gap: '1.5rem', borderRadius: 'var(--radius-md)' }}>
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Net Worth</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: netWorth.netWorth >= 0 ? '#10B981' : '#EF4444' }}>
                ${netWorth.netWorth.toFixed(2)}
              </div>
            </div>
            <div style={{ borderLeft: '1px solid var(--border-glass)', paddingLeft: '1rem' }}>
              <div style={{ fontSize: '0.75rem', color: '#10B981' }}>Assets: ${netWorth.totalAssets.toFixed(2)}</div>
              <div style={{ fontSize: '0.75rem', color: '#EF4444' }}>Liabilities: ${netWorth.totalLiabilities.toFixed(2)}</div>
            </div>
          </div>
        )}
      </div>

      {/* Module Navigation Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '2rem', borderBottom: '1px solid var(--border-glass)', paddingBottom: '0.75rem' }}>
        <button
          onClick={() => setActiveTab('overview')}
          className={`btn ${activeTab === 'overview' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '0.5rem 1.25rem' }}
        >
          <PieChart size={16} /> Overview & Analytics
        </button>
        <button
          onClick={() => setActiveTab('invoices')}
          className={`btn ${activeTab === 'invoices' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '0.5rem 1.25rem', position: 'relative' }}
        >
          <FileText size={16} /> ⭐ Activity-Linked Invoices
        </button>
        <button
          onClick={() => setActiveTab('budgets')}
          className={`btn ${activeTab === 'budgets' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '0.5rem 1.25rem' }}
        >
          <TrendingUp size={16} /> Budgets & Spending Caps
        </button>
        <button
          onClick={() => setActiveTab('subscriptions')}
          className={`btn ${activeTab === 'subscriptions' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '0.5rem 1.25rem' }}
        >
          <Repeat size={16} /> Subscription Hub
        </button>
        <button
          onClick={() => setActiveTab('tax')}
          className={`btn ${activeTab === 'tax' ? 'btn-primary' : 'btn-secondary'}`}
          style={{ padding: '0.5rem 1.25rem' }}
        >
          <Receipt size={16} /> Tax & Deductibles
        </button>
      </div>

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODULE 1: OVERVIEW & ANALYTICS */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      {activeTab === 'overview' && (
        <div>
          {/* AI Insights Banner */}
          {aiInsights.length > 0 && (
            <div className="glass-card" style={{ padding: '1.25rem', marginBottom: '2rem', background: 'rgba(168, 85, 247, 0.1)', borderColor: 'rgba(168, 85, 247, 0.3)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, color: '#D8B4FE', marginBottom: '0.5rem' }}>
                <Sparkles size={18} /> AI Telemetry Insights & Spending Forecast
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', fontSize: '0.9rem', color: '#F3E8FF' }}>
                {aiInsights.map((insight, idx) => (
                  <div key={idx}>{insight}</div>
                ))}
              </div>
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '2rem', marginBottom: '2rem' }}>
            {/* Quick Log Form */}
            <div className="glass-card" style={{ padding: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <PlusCircle size={18} color="var(--primary)" /> Log Expense Entry
              </h3>

              {/* Receipt Image Scanner */}
              <div style={{ marginBottom: '1.25rem', padding: '0.75rem', background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-sm)', border: '1px dashed var(--border-glass)' }}>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.35rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <Receipt size={14} /> Scan Receipt Image (OCR Simulator)
                </div>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <input
                    type="text"
                    placeholder="e.g. starbucks_receipt.jpg or uber_receipt.png"
                    value={scanFilename}
                    onChange={(e) => setScanFilename(e.target.value)}
                    className="form-control"
                    style={{ fontSize: '0.8rem', padding: '0.35rem 0.6rem' }}
                  />
                  <button type="button" onClick={handleScanReceipt} className="btn btn-secondary" style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}>
                    OCR Scan
                  </button>
                </div>
              </div>

              <form onSubmit={handleLogExpense} style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                <div className="form-group">
                  <label className="form-label">Category</label>
                  <select value={category} onChange={(e) => setCategory(e.target.value)} className="form-control">
                    <option value="FOOD">Food & Dining</option>
                    <option value="RENT">Housing & Rent</option>
                    <option value="BILLS">Utilities & Bills</option>
                    <option value="ENTERTAINMENT">Entertainment & Leisure</option>
                    <option value="TRAVEL">Travel & Transport</option>
                    <option value="BUSINESS">Business & Work</option>
                    <option value="SUBSCRIPTION">SaaS Subscription</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Payment Source Account</label>
                  <select value={accountType} onChange={(e) => setAccountType(e.target.value)} className="form-control">
                    <option value="CASH">Cash Wallet</option>
                    <option value="WALLET">PayGuard Digital Wallet (Auto-Debit)</option>
                    <option value="BANK">Bank Account</option>
                    <option value="CREDIT">Credit Card (Liability)</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Amount ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    placeholder="0.00"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    required
                    className="form-control"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Merchant Name</label>
                  <input
                    type="text"
                    placeholder="e.g. Whole Foods, Amazon, Starbucks"
                    value={merchantName}
                    onChange={(e) => setMerchantName(e.target.value)}
                    className="form-control"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Tags (sub-labels)</label>
                  <input
                    type="text"
                    placeholder="e.g. #vacation2026,#client_alpha"
                    value={tags}
                    onChange={(e) => setTags(e.target.value)}
                    className="form-control"
                  />
                </div>

                <div style={{ display: 'flex', gap: '1rem', marginTop: '0.25rem' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.85rem', cursor: 'pointer' }}>
                    <input type="checkbox" checked={taxDeductible} onChange={(e) => setTaxDeductible(e.target.checked)} />
                    Tax Deductible
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.85rem', cursor: 'pointer' }}>
                    <input type="checkbox" checked={recurring} onChange={(e) => setRecurring(e.target.checked)} />
                    Recurring
                  </label>
                </div>

                <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem' }}>
                  <PlusCircle size={16} /> Record Expense Entry
                </button>
              </form>
            </div>

            {/* Summary Cards & Expense Table */}
            <div>
              {summary && (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
                  <div className="glass-card" style={{ padding: '1.25rem' }}>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Total Spent</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)' }}>${summary.totalExpenses.toFixed(2)}</div>
                  </div>
                  <div className="glass-card" style={{ padding: '1.25rem' }}>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Tax Deductibles</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#3B82F6' }}>${summary.totalTaxDeductible.toFixed(2)}</div>
                  </div>
                  <div className="glass-card" style={{ padding: '1.25rem' }}>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Log Entries</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#10B981' }}>{expenses.length}</div>
                  </div>
                </div>
              )}

              {/* Expense History Table with Instant Search */}
              <div className="glass-card" style={{ padding: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Recorded Expense Log</h3>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'rgba(255,255,255,0.05)', padding: '0.35rem 0.75rem', borderRadius: 'var(--radius-sm)' }}>
                    <Search size={14} color="var(--text-muted)" />
                    <input
                      type="text"
                      placeholder="Search name, tag, category..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      style={{ background: 'transparent', border: 'none', color: '#FFF', fontSize: '0.85rem', outline: 'none' }}
                    />
                  </div>
                </div>

                <div className="table-container">
                  <table>
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Merchant</th>
                        <th>Category</th>
                        <th>Source</th>
                        <th>Amount</th>
                        <th>Flags</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredExpenses.length === 0 ? (
                        <tr>
                          <td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                            No expenses logged yet.
                          </td>
                        </tr>
                      ) : (
                        filteredExpenses.map((exp) => (
                          <tr key={exp.id}>
                            <td>{new Date(exp.createdAt).toLocaleDateString()}</td>
                            <td style={{ fontWeight: 600 }}>{exp.merchantName}</td>
                            <td>
                              <span style={{ padding: '0.2rem 0.5rem', borderRadius: '4px', background: 'rgba(255,255,255,0.08)', fontSize: '0.75rem', fontWeight: 600 }}>
                                {exp.category}
                              </span>
                            </td>
                            <td>{exp.accountType}</td>
                            <td style={{ fontWeight: 700, color: '#EF4444' }}>-${exp.amount.toFixed(2)}</td>
                            <td>
                              {exp.taxDeductible && <span style={{ color: '#3B82F6', fontSize: '0.75rem', marginRight: '0.35rem' }}>[Tax]</span>}
                              {exp.tags && <span style={{ color: '#A855F7', fontSize: '0.75rem' }}>{exp.tags}</span>}
                            </td>
                            <td>
                              <button onClick={() => handleDeleteExpense(exp.id)} className="btn btn-secondary" style={{ padding: '0.2rem 0.4rem', color: '#EF4444' }}>
                                <Trash2 size={14} />
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODULE 2: ⭐ ACTIVITY-LINKED INVOICES */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      {activeTab === 'invoices' && (
        <div>
          <div className="glass-card" style={{ padding: '1.5rem', marginBottom: '2rem', background: 'rgba(59, 130, 246, 0.08)', borderColor: 'rgba(59, 130, 246, 0.3)' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#93C5FD', marginBottom: '0.35rem' }}>
              ⭐ Unique Feature: Activity-Linked Invoices
            </h3>
            <p style={{ fontSize: '0.9rem', color: '#BFDBFE' }}>
              Unlike traditional apps that separate entries, PayGuard allows you to link Income and Expenses for specific activities/projects (e.g. "NYC Tech Conference 2026" or "Client Redesign Project") into a single activity invoice ledger, calculating net profit and margin % automatically.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1.8fr', gap: '2rem' }}>
            {/* Invoice Builder Form */}
            <div className="glass-card" style={{ padding: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Create Activity Invoice</h3>

              <form onSubmit={handleCreateActivityInvoice} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Activity / Project Title</label>
                  <input
                    type="text"
                    placeholder="e.g. NYC Tech Conference 2026"
                    value={activityTitle}
                    onChange={(e) => setActivityTitle(e.target.value)}
                    required
                    className="form-control"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Client / Sponsor Name</label>
                  <input
                    type="text"
                    placeholder="e.g. Acme Corp Inc."
                    value={clientName}
                    onChange={(e) => setClientName(e.target.value)}
                    className="form-control"
                  />
                </div>

                {/* Line Items Editor */}
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                    <label className="form-label" style={{ marginBottom: 0 }}>Line Items (Income & Expenses)</label>
                    <div style={{ display: 'flex', gap: '0.35rem' }}>
                      <button type="button" onClick={() => addLineItem('INCOME')} className="btn btn-secondary" style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', color: '#10B981' }}>
                        + Income Item
                      </button>
                      <button type="button" onClick={() => addLineItem('EXPENSE')} className="btn btn-secondary" style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', color: '#EF4444' }}>
                        + Expense Item
                      </button>
                    </div>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {lineItems.map((item, idx) => (
                      <div key={idx} style={{ display: 'grid', gridTemplateColumns: '0.8fr 1.5fr 1fr 0.3fr', gap: '0.35rem', alignItems: 'center', background: 'rgba(255,255,255,0.03)', padding: '0.5rem', borderRadius: '4px' }}>
                        <select
                          value={item.type}
                          onChange={(e) => updateLineItem(idx, 'type', e.target.value)}
                          className="form-control"
                          style={{ fontSize: '0.75rem', padding: '0.25rem' }}
                        >
                          <option value="INCOME">INCOME</option>
                          <option value="EXPENSE">EXPENSE</option>
                        </select>
                        <input
                          type="text"
                          value={item.description}
                          onChange={(e) => updateLineItem(idx, 'description', e.target.value)}
                          placeholder="Description"
                          className="form-control"
                          style={{ fontSize: '0.75rem', padding: '0.25rem' }}
                        />
                        <input
                          type="number"
                          value={item.amount}
                          onChange={(e) => updateLineItem(idx, 'amount', parseFloat(e.target.value) || 0)}
                          placeholder="Amount"
                          className="form-control"
                          style={{ fontSize: '0.75rem', padding: '0.25rem' }}
                        />
                        <button type="button" onClick={() => removeLineItem(idx)} style={{ background: 'none', border: 'none', color: '#EF4444', cursor: 'pointer' }}>
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Net Margin Live Calculation Badge */}
                <div style={{ padding: '0.75rem', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: 'var(--radius-sm)', fontSize: '0.85rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>Total Income: <strong>${previewIncome.toFixed(2)}</strong></span>
                    <span>Total Expenses: <strong>${previewExpense.toFixed(2)}</strong></span>
                  </div>
                  <div style={{ marginTop: '0.35rem', fontWeight: 800, color: previewNet >= 0 ? '#10B981' : '#EF4444' }}>
                    Net Profit: ${previewNet.toFixed(2)} ({previewMargin}% Margin)
                  </div>
                </div>

                <button type="submit" className="btn btn-primary">
                  <FileText size={16} /> Finalize Activity Invoice
                </button>
              </form>
            </div>

            {/* Existing Activity Invoices List */}
            <div className="glass-card" style={{ padding: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Finalized Activity Invoices</h3>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {invoices.length === 0 ? (
                  <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '2rem' }}>
                    No activity invoices created yet.
                  </div>
                ) : (
                  invoices.map((inv) => (
                    <div key={inv.id} style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid var(--border-glass)', padding: '1.25rem', borderRadius: 'var(--radius-md)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                        <div>
                          <div style={{ fontWeight: 800, fontSize: '1.1rem' }}>{inv.activityTitle}</div>
                          {inv.clientName && <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Client: {inv.clientName}</div>}
                        </div>
                        <button onClick={() => activityInvoiceApi.exportCsv(inv.id, inv.activityTitle)} className="btn btn-secondary" style={{ padding: '0.3rem 0.65rem', fontSize: '0.75rem' }}>
                          <Download size={14} /> Export CSV Statement
                        </button>
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.5rem', fontSize: '0.85rem', marginBottom: '0.75rem' }}>
                        <div>Income: <strong style={{ color: '#10B981' }}>+${inv.totalIncome.toFixed(2)}</strong></div>
                        <div>Expenses: <strong style={{ color: '#EF4444' }}>-${inv.totalExpenses.toFixed(2)}</strong></div>
                        <div>Net Margin: <strong style={{ color: inv.netProfit >= 0 ? '#10B981' : '#EF4444' }}>${inv.netProfit.toFixed(2)} ({inv.profitMarginPercent}%)</strong></div>
                      </div>

                      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        Line Items ({inv.lineItems.length}): {inv.lineItems.map((i) => `${i.description} ($${i.amount})`).join(', ')}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODULE 3: BUDGETS & SPENDING CAPS */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      {activeTab === 'budgets' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '2rem' }}>
          {/* Configure Budget Cap */}
          <div className="glass-card" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Configure Category Cap</h3>

            <form onSubmit={handleSetBudget} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div className="form-group">
                <label className="form-label">Category</label>
                <select value={budgetCategory} onChange={(e) => setBudgetCategory(e.target.value)} className="form-control">
                  <option value="FOOD">Food & Dining</option>
                  <option value="RENT">Housing & Rent</option>
                  <option value="BILLS">Utilities & Bills</option>
                  <option value="ENTERTAINMENT">Entertainment & Leisure</option>
                  <option value="TRAVEL">Travel & Transport</option>
                  <option value="BUSINESS">Business & Work</option>
                  <option value="SUBSCRIPTION">SaaS Subscriptions</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Monthly Spending Cap ($)</label>
                <input
                  type="number"
                  placeholder="e.g. 500"
                  value={budgetCap}
                  onChange={(e) => setBudgetCap(e.target.value)}
                  required
                  className="form-control"
                />
              </div>

              <button type="submit" className="btn btn-primary">
                <TrendingUp size={16} /> Save Category Cap
              </button>
            </form>
          </div>

          {/* Budget Progress Bars */}
          <div className="glass-card" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Monthly Spending Progress & Alert Thresholds</h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              {budgets.length === 0 ? (
                <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '2rem' }}>
                  No category budgets configured yet.
                </div>
              ) : (
                budgets.map((b) => {
                  let barColor = '#10B981';
                  if (b.statusPill === 'WARNING_80') barColor = '#F59E0B';
                  if (b.statusPill === 'OVERSPENT_100') barColor = '#EF4444';

                  return (
                    <div key={b.id}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem', fontSize: '0.9rem' }}>
                        <span style={{ fontWeight: 700 }}>{b.category}</span>
                        <span>
                          <strong>${b.spentAmount.toFixed(2)}</strong> / ${b.monthlyCap.toFixed(2)} ({b.percentUsed.toFixed(1)}%)
                        </span>
                      </div>

                      {/* Progress Bar Container */}
                      <div style={{ height: '10px', background: 'rgba(255,255,255,0.1)', borderRadius: '5px', overflow: 'hidden' }}>
                        <div style={{ height: '100%', width: `${Math.min(b.percentUsed, 100)}%`, background: barColor, transition: 'width 0.5s ease' }} />
                      </div>

                      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.35rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        <span>Status: <strong style={{ color: barColor }}>{b.statusPill}</strong></span>
                        <span>Remaining: ${b.remainingAmount.toFixed(2)}</span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODULE 4: SUBSCRIPTION HUB */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      {activeTab === 'subscriptions' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '2rem' }}>
          {/* Add Subscription Form */}
          <div className="glass-card" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Track SaaS Subscription</h3>

            <form onSubmit={handleAddSubscription} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div className="form-group">
                <label className="form-label">Service Name</label>
                <input
                  type="text"
                  placeholder="e.g. Netflix, AWS Cloud, Spotify"
                  value={subServiceName}
                  onChange={(e) => setSubServiceName(e.target.value)}
                  required
                  className="form-control"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Monthly Cost ($)</label>
                <input
                  type="number"
                  step="0.01"
                  placeholder="14.99"
                  value={subAmount}
                  onChange={(e) => setSubAmount(e.target.value)}
                  required
                  className="form-control"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Next Renewal Date</label>
                <input
                  type="date"
                  value={subRenewalDate}
                  onChange={(e) => setSubRenewalDate(e.target.value)}
                  required
                  className="form-control"
                />
              </div>

              <button type="submit" className="btn btn-primary">
                <Repeat size={16} /> Add Subscription
              </button>
            </form>
          </div>

          {/* Subscriptions List */}
          <div className="glass-card" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Active Subscriptions & Renewal Countdown</h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {subscriptions.length === 0 ? (
                <div style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '2rem' }}>
                  No subscriptions tracked yet.
                </div>
              ) : (
                subscriptions.map((s) => (
                  <div key={s.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(255,255,255,0.03)', padding: '1rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-glass)' }}>
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '1rem' }}>{s.serviceName}</div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        Renewing: {s.nextRenewalDate} ({s.daysUntilRenewal} days remaining)
                      </div>
                      {s.cancellationNotes && <div style={{ fontSize: '0.75rem', color: '#F59E0B' }}>Notes: {s.cancellationNotes}</div>}
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                      <div style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--primary)' }}>
                        ${s.amount.toFixed(2)}/mo
                      </div>
                      {s.status === 'ACTIVE' ? (
                        <button onClick={() => handleCancelSubscription(s.id)} className="btn btn-secondary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem', color: '#EF4444' }}>
                          1-Click Cancel
                        </button>
                      ) : (
                        <span style={{ fontSize: '0.75rem', color: '#F59E0B', fontWeight: 600 }}>[Cancel Requested]</span>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODULE 5: TAX & DEDUCTIBLES */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      {activeTab === 'tax' && (
        <div className="glass-card" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 800 }}>Business Tax-Deductible Expense Report</h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                Filtered list of all expenses flagged as tax-deductible for quarterly accountant filing
              </p>
            </div>
          </div>

          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Merchant</th>
                  <th>Category</th>
                  <th>Source</th>
                  <th>Amount</th>
                  <th>Tags</th>
                </tr>
              </thead>
              <tbody>
                {expenses.filter((e) => e.taxDeductible).length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                      No tax-deductible expenses logged.
                    </td>
                  </tr>
                ) : (
                  expenses
                    .filter((e) => e.taxDeductible)
                    .map((exp) => (
                      <tr key={exp.id}>
                        <td>{new Date(exp.createdAt).toLocaleDateString()}</td>
                        <td style={{ fontWeight: 600 }}>{exp.merchantName}</td>
                        <td>{exp.category}</td>
                        <td>{exp.accountType}</td>
                        <td style={{ fontWeight: 700, color: '#3B82F6' }}>${exp.amount.toFixed(2)}</td>
                        <td>{exp.tags}</td>
                      </tr>
                    ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
