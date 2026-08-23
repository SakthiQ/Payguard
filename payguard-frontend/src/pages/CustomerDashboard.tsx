import React, { useState, useEffect } from 'react';
import { walletApi } from '../api/walletApi';
import type { WalletResponse } from '../api/walletApi';
import { txApi } from '../api/txApi';
import type { TransactionItem } from '../api/txApi';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { Send, PlusCircle, RefreshCw, AlertTriangle, CheckCircle2, History, CreditCard, Download } from 'lucide-react';

export const CustomerDashboard: React.FC = () => {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [history, setHistory] = useState<TransactionItem[]>([]);
  const [loading, setLoading] = useState(true);

  // Deposit Modal State
  const [isDepositOpen, setIsDepositOpen] = useState(false);
  const [depositAmount, setDepositAmount] = useState('10000.00');
  const [depositLoading, setDepositLoading] = useState(false);

  // Transfer Form State
  const [recipientAccount, setRecipientAccount] = useState('');
  const [transferAmount, setTransferAmount] = useState('');
  const [description, setDescription] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState(() => `IDEM-${Math.random().toString(36).substring(2, 9).toUpperCase()}`);
  const [transferLoading, setTransferLoading] = useState(false);

  // Transfer Feedback Banner
  const [feedback, setFeedback] = useState<{ type: 'success' | 'warning' | 'error'; message: string; ref?: string } | null>(null);

  const loadData = async () => {
    try {
      const [walletData, historyData] = await Promise.all([
        walletApi.getMyWallet(),
        txApi.getHistory(),
      ]);
      setWallet(walletData);
      setHistory(historyData);
    } catch (err: any) {
      console.error('Failed to load dashboard data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleDeposit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!wallet) return;
    setDepositLoading(true);
    try {
      const res = await walletApi.deposit(wallet.walletId, parseFloat(depositAmount));
      setWallet((prev) => prev ? { ...prev, balance: res.newBalance } : null);
      setFeedback({ type: 'success', message: res.message, ref: res.transactionReference });
      setIsDepositOpen(false);
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Deposit failed');
    } finally {
      setDepositLoading(false);
    }
  };

  const handleTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    setFeedback(null);
    setTransferLoading(true);

    try {
      const res = await txApi.processTransfer(
        {
          recipientAccountNumber: recipientAccount,
          amount: parseFloat(transferAmount),
          description,
        },
        idempotencyKey
      );

      if (res.status === 'FLAGGED') {
        setFeedback({
          type: 'warning',
          message: res.message,
          ref: res.transactionReference,
        });
      } else {
        setFeedback({
          type: 'success',
          message: res.message,
          ref: res.transactionReference,
        });
      }

      // Generate fresh idempotency key for next transaction
      setIdempotencyKey(`IDEM-${Math.random().toString(36).substring(2, 9).toUpperCase()}`);
      setRecipientAccount('');
      setTransferAmount('');
      setDescription('');
      loadData();
    } catch (err: any) {
      setFeedback({
        type: 'error',
        message: err.response?.data?.message || 'Transfer failed. Check recipient account number and available balance.',
      });
    } finally {
      setTransferLoading(false);
    }
  };

  const regenerateKey = () => {
    setIdempotencyKey(`IDEM-${Math.random().toString(36).substring(2, 9).toUpperCase()}`);
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <RefreshCw className="animate-spin" size={32} color="var(--primary)" />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '2rem 1.5rem' }}>
      
      {/* Top Banner: Wallet Overview */}
      {wallet && (
        <div className="glass-card animate-fade-in" style={{
          padding: '2rem',
          marginBottom: '2rem',
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '1.5rem',
          alignItems: 'center',
          background: 'linear-gradient(135deg, rgba(17, 24, 39, 0.9) 0%, rgba(31, 41, 55, 0.8) 100%)',
          border: '1px solid rgba(99, 102, 241, 0.25)',
        }}>
          <div>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Available Balance
            </span>
            <div style={{ fontSize: '2.5rem', fontWeight: 800, color: '#F9FAFB', letterSpacing: '-0.03em', marginTop: '0.25rem' }}>
              ${wallet.balance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 4 })}
              <span style={{ fontSize: '1rem', color: 'var(--accent-cyan)', marginLeft: '0.5rem', fontWeight: 600 }}>
                {wallet.currency}
              </span>
            </div>
          </div>

          <div>
            <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Account Number
            </span>
            <div style={{ fontSize: '1.25rem', fontWeight: 700, fontFamily: 'monospace', marginTop: '0.25rem', color: '#E5E7EB' }}>
              {wallet.accountNumber}
            </div>
            <div style={{ marginTop: '0.4rem' }}>
              <StatusBadge status={wallet.status} />
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
            <button onClick={() => setIsDepositOpen(true)} className="btn btn-success" style={{ padding: '0.8rem 1.4rem' }}>
              <PlusCircle size={18} /> Top-up Sandbox Wallet
            </button>
          </div>
        </div>
      )}

      {/* Main Grid: Transfer Form & Quick Actions */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginBottom: '2.5rem' }}>
        
        {/* P2P Transfer Card */}
        <div className="glass-card" style={{ padding: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <div style={{
              width: '40px',
              height: '40px',
              borderRadius: '10px',
              background: 'rgba(99, 102, 241, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--primary)',
            }}>
              <Send size={22} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700 }}>Send Money (P2P Transfer)</h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Idempotent peer-to-peer transaction protocol</p>
            </div>
          </div>

          {/* Feedback Banner */}
          {feedback && (
            <div style={{
              padding: '0.85rem 1rem',
              borderRadius: 'var(--radius-sm)',
              marginBottom: '1.25rem',
              fontSize: '0.85rem',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '0.65rem',
              background: feedback.type === 'success' ? 'rgba(16, 185, 129, 0.15)' : feedback.type === 'warning' ? 'rgba(245, 158, 11, 0.15)' : 'rgba(239, 68, 68, 0.15)',
              border: `1px solid ${feedback.type === 'success' ? 'rgba(16, 185, 129, 0.3)' : feedback.type === 'warning' ? 'rgba(245, 158, 11, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
              color: feedback.type === 'success' ? '#34D399' : feedback.type === 'warning' ? '#FBBF24' : '#F87171',
            }}>
              {feedback.type === 'success' && <CheckCircle2 size={20} style={{ flexShrink: 0 }} />}
              {feedback.type === 'warning' && <AlertTriangle size={20} style={{ flexShrink: 0 }} />}
              {feedback.type === 'error' && <AlertTriangle size={20} style={{ flexShrink: 0 }} />}
              <div>
                <div style={{ fontWeight: 600 }}>{feedback.message}</div>
                {feedback.ref && <div style={{ fontSize: '0.75rem', fontFamily: 'monospace', opacity: 0.9, marginTop: '0.25rem' }}>Ref: {feedback.ref}</div>}
              </div>
            </div>
          )}

          <form onSubmit={handleTransfer}>
            <div className="form-group">
              <label className="form-label">Recipient Account Number</label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. ACC-3933245836"
                value={recipientAccount}
                onChange={(e) => setRecipientAccount(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Transfer Amount ($)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                className="form-control"
                placeholder="100.00"
                value={transferAmount}
                onChange={(e) => setTransferAmount(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Description (Optional)</label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. Dinner split, Invoice payment"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            {/* Idempotency Key Info */}
            <div style={{
              background: 'rgba(31, 41, 55, 0.5)',
              padding: '0.75rem 1rem',
              borderRadius: 'var(--radius-sm)',
              marginBottom: '1.5rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              border: '1px solid var(--border-color)',
            }}>
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>IDEMPOTENCY KEY</div>
                <div style={{ fontSize: '0.85rem', fontFamily: 'monospace', color: 'var(--accent-cyan)' }}>{idempotencyKey}</div>
              </div>
              <button type="button" onClick={regenerateKey} className="btn btn-secondary" style={{ padding: '0.35rem 0.65rem', fontSize: '0.75rem' }}>
                <RefreshCw size={13} /> New Key
              </button>
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', padding: '0.85rem' }} disabled={transferLoading || wallet?.status !== 'ACTIVE'}>
              {transferLoading ? 'Processing Transfer...' : (
                <>
                  <Send size={18} /> Confirm & Execute Transfer
                </>
              )}
            </button>
          </form>
        </div>

        {/* Sandbox Guide & Features Card */}
        <div className="glass-card" style={{ padding: '2rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.25rem' }}>
              <div style={{
                width: '40px',
                height: '40px',
                borderRadius: '10px',
                background: 'rgba(6, 182, 212, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--accent-cyan)',
              }}>
                <CreditCard size={22} />
              </div>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700 }}>Sandbox Protocol Instructions</h3>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Deterministic Fraud & Transaction Rules</p>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', fontSize: '0.9rem' }}>
              <div style={{ background: 'rgba(31, 41, 55, 0.4)', padding: '1rem', borderRadius: 'var(--radius-sm)', borderLeft: '3px solid var(--primary)' }}>
                <strong style={{ color: '#F9FAFB' }}>1. Idempotent Transfers</strong>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.2rem' }}>
                  Retrying a transfer with the exact same Idempotency Key returns the cached completion payload without re-executing money movement.
                </p>
              </div>

              <div style={{ background: 'rgba(31, 41, 55, 0.4)', padding: '1rem', borderRadius: 'var(--radius-sm)', borderLeft: '3px solid var(--warning)' }}>
                <strong style={{ color: '#F9FAFB' }}>2. High Amount Fraud Rule ($5,000 Threshold)</strong>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.2rem' }}>
                  Any transfer of $5,000.00 or higher triggers the <code style={{ color: 'var(--warning)' }}>HIGH_AMOUNT</code> rule, placing the transaction into status <code style={{ color: 'var(--warning)' }}>FLAGGED</code> for Fraud Analyst review.
                </p>
              </div>

              <div style={{ background: 'rgba(31, 41, 55, 0.4)', padding: '1rem', borderRadius: 'var(--radius-sm)', borderLeft: '3px solid var(--accent-cyan)' }}>
                <strong style={{ color: '#F9FAFB' }}>3. High Velocity Fraud Rule (3 Transfers / 300s)</strong>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '0.2rem' }}>
                  Attempting 3 or more transfers within a 5-minute window triggers <code style={{ color: 'var(--warning)' }}>HIGH_VELOCITY</code> risk queuing.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Transaction History Section */}
      <div className="glass-card" style={{ padding: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <History size={22} color="var(--primary)" />
            <h3 style={{ fontSize: '1.2rem', fontWeight: 700 }}>Transaction History</h3>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button onClick={() => txApi.downloadCsvStatement()} className="btn btn-secondary" style={{ padding: '0.4rem 0.85rem', fontSize: '0.85rem' }}>
              <Download size={14} /> Export CSV Statement
            </button>
            <button onClick={loadData} className="btn btn-secondary" style={{ padding: '0.4rem 0.85rem', fontSize: '0.85rem' }}>
              <RefreshCw size={14} /> Refresh Table
            </button>
          </div>
        </div>

        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Reference</th>
                <th>Type</th>
                <th>Sender Account</th>
                <th>Recipient Account</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {history.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                    No transactions found. Make your first deposit or transfer above!
                  </td>
                </tr>
              ) : (
                history.map((tx) => (
                  <tr key={tx.id}>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--accent-cyan)' }}>{tx.transactionReference}</td>
                    <td style={{ fontWeight: 600 }}>{tx.type}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{tx.senderAccountNumber}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{tx.recipientAccountNumber}</td>
                    <td style={{ fontWeight: 700, color: tx.type === 'DEPOSIT' ? '#34D399' : '#F9FAFB' }}>
                      ${tx.amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 4 })}
                    </td>
                    <td><StatusBadge status={tx.status} /></td>
                    <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{new Date(tx.createdAt).toLocaleString()}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Top-up Deposit Modal */}
      <Modal isOpen={isDepositOpen} onClose={() => setIsDepositOpen(false)} title="Sandbox Wallet Top-up">
        <form onSubmit={handleDeposit}>
          <div className="form-group">
            <label className="form-label">Deposit Amount ($USD)</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              className="form-control"
              value={depositAmount}
              onChange={(e) => setDepositAmount(e.target.value)}
              required
            />
          </div>

          <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
            {['1000', '5000', '10000', '25000'].map((amt) => (
              <button
                key={amt}
                type="button"
                className="btn btn-secondary"
                style={{ flex: 1, padding: '0.4rem', fontSize: '0.8rem' }}
                onClick={() => setDepositAmount(amt)}
              >
                +${amt}
              </button>
            ))}
          </div>

          <button type="submit" className="btn btn-success" style={{ width: '100%', padding: '0.75rem' }} disabled={depositLoading}>
            {depositLoading ? 'Processing Top-up...' : 'Confirm Deposit'}
          </button>
        </form>
      </Modal>

    </div>
  );
};
