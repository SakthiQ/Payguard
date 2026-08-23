import React, { useState, useEffect } from 'react';
import { fraudApi } from '../api/fraudApi';
import type { FraudFlagItem } from '../api/fraudApi';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { ShieldAlert, CheckCircle2, XCircle, RefreshCw, FileText } from 'lucide-react';

export const FraudAnalystPage: React.FC = () => {
  const [flags, setFlags] = useState<FraudFlagItem[]>([]);
  const [loading, setLoading] = useState(true);

  // Review Modal State
  const [selectedFlag, setSelectedFlag] = useState<FraudFlagItem | null>(null);
  const [action, setAction] = useState<'APPROVE' | 'DECLINE'>('APPROVE');
  const [notes, setNotes] = useState('');
  const [reviewLoading, setReviewLoading] = useState(false);

  const loadFlags = async () => {
    try {
      const data = await fraudApi.getPendingFlags();
      setFlags(data);
    } catch (err) {
      console.error('Failed to load pending fraud flags', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFlags();
  }, []);

  const handleReviewSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFlag) return;
    setReviewLoading(true);

    try {
      await fraudApi.reviewFlag(selectedFlag.flagId, {
        action,
        notes,
      });
      setSelectedFlag(null);
      setNotes('');
      loadFlags();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to submit fraud review');
    } finally {
      setReviewLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <RefreshCw className="animate-spin" size={32} color="var(--warning)" />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '2rem 1.5rem' }}>
      
      {/* Page Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <div style={{
            width: '46px',
            height: '46px',
            borderRadius: '12px',
            background: 'rgba(245, 158, 11, 0.15)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--warning)',
            boxShadow: '0 0 20px rgba(245, 158, 11, 0.3)'
          }}>
            <ShieldAlert size={26} />
          </div>
          <div>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 800, letterSpacing: '-0.02em' }}>Fraud Review Queue</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              Risk evaluation inspection & analyst decision workflow
            </p>
          </div>
        </div>

        <button onClick={loadFlags} className="btn btn-secondary">
          <RefreshCw size={16} /> Refresh Queue
        </button>
      </div>

      {/* Pending Queue List */}
      <div className="glass-card" style={{ padding: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>
            Pending Risk Flags ({flags.length})
          </h3>
        </div>

        {flags.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-muted)' }}>
            <CheckCircle2 size={48} color="var(--success)" style={{ marginBottom: '1rem', opacity: 0.8 }} />
            <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-main)' }}>Queue Clear</h4>
            <p style={{ fontSize: '0.9rem', marginTop: '0.25rem' }}>There are currently no transactions flagged for risk evaluation.</p>
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Flag ID</th>
                  <th>Transaction Reference</th>
                  <th>Rule Triggered</th>
                  <th>Risk Score</th>
                  <th>Amount</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {flags.map((flag) => (
                  <tr key={flag.flagId}>
                    <td style={{ fontWeight: 700 }}>#{flag.flagId}</td>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--accent-cyan)' }}>{flag.transactionReference}</td>
                    <td>
                      <span className="badge badge-flagged">{flag.triggeredRuleCode}</span>
                    </td>
                    <td>
                      <span style={{
                        padding: '0.2rem 0.5rem',
                        borderRadius: '6px',
                        fontWeight: 800,
                        fontSize: '0.85rem',
                        background: 'rgba(239, 68, 68, 0.2)',
                        color: '#F87171',
                        border: '1px solid rgba(239, 68, 68, 0.4)',
                      }}>
                        {flag.riskScore} / 100
                      </span>
                    </td>
                    <td style={{ fontWeight: 800 }}>${flag.amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                    <td style={{ fontSize: '0.85rem', color: 'var(--text-muted)', maxWidth: '280px' }}>{flag.flagReason}</td>
                    <td><StatusBadge status={flag.status} /></td>
                    <td>
                      <button
                        onClick={() => {
                          setSelectedFlag(flag);
                          setAction('APPROVE');
                          setNotes('');
                        }}
                        className="btn btn-primary"
                        style={{ padding: '0.4rem 0.75rem', fontSize: '0.8rem' }}
                      >
                        <FileText size={14} /> Inspect & Review
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Review Modal */}
      <Modal isOpen={!!selectedFlag} onClose={() => setSelectedFlag(null)} title={`Review Fraud Flag #${selectedFlag?.flagId}`}>
        {selectedFlag && (
          <form onSubmit={handleReviewSubmit}>
            <div style={{
              background: 'rgba(31, 41, 55, 0.5)',
              padding: '1rem',
              borderRadius: 'var(--radius-sm)',
              marginBottom: '1.25rem',
              fontSize: '0.85rem',
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: '0.75rem',
              border: '1px solid var(--border-color)',
            }}>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Transaction Ref:</span>
                <div style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--accent-cyan)' }}>{selectedFlag.transactionReference}</div>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Amount:</span>
                <div style={{ fontWeight: 800, color: 'var(--text-main)' }}>${selectedFlag.amount.toFixed(2)}</div>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Triggered Rule:</span>
                <div style={{ fontWeight: 700, color: '#FBBF24' }}>{selectedFlag.triggeredRuleCode} (Score: {selectedFlag.riskScore})</div>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Sender Account:</span>
                <div style={{ fontFamily: 'monospace' }}>{selectedFlag.senderAccountNumber}</div>
              </div>
            </div>

            {selectedFlag.aiRiskInsight && (
              <div style={{
                background: 'rgba(99, 102, 241, 0.12)',
                border: '1px solid rgba(99, 102, 241, 0.3)',
                padding: '1rem',
                borderRadius: 'var(--radius-sm)',
                marginBottom: '1.25rem',
                fontSize: '0.85rem',
                color: '#E0E7FF',
                lineHeight: '1.5',
              }}>
                <div style={{ fontWeight: 700, color: 'var(--accent-cyan)', marginBottom: '0.35rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <span>🤖 AI Risk Telemetry & Analyst Decision Support</span>
                </div>
                <div>{selectedFlag.aiRiskInsight}</div>
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Review Action Decision</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <button
                  type="button"
                  className={`btn ${action === 'APPROVE' ? 'btn-success' : 'btn-secondary'}`}
                  onClick={() => setAction('APPROVE')}
                  style={{ padding: '0.75rem' }}
                >
                  <CheckCircle2 size={18} /> APPROVE Transfer
                </button>
                <button
                  type="button"
                  className={`btn ${action === 'DECLINE' ? 'btn-danger' : 'btn-secondary'}`}
                  onClick={() => setAction('DECLINE')}
                  style={{ padding: '0.75rem' }}
                >
                  <XCircle size={18} /> DECLINE Transfer
                </button>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Analyst Investigation Notes (Required)</label>
              <textarea
                className="form-control"
                rows={3}
                placeholder="Detail verification steps, phone confirmation, or compliance risk reasoning..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', padding: '0.85rem' }} disabled={reviewLoading}>
              {reviewLoading ? 'Submitting Review...' : `Submit Decision (${action})`}
            </button>
          </form>
        )}
      </Modal>

    </div>
  );
};
