import React, { useState, useEffect } from 'react';
import { adminApi } from '../api/adminApi';
import type { AdminUserItem, FraudRuleItem, AuditLogDocument } from '../api/adminApi';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { Settings, Users, ShieldCheck, Database, Lock, Unlock, Edit3, RefreshCw } from 'lucide-react';

export const AdminPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'users' | 'rules' | 'audit'>('users');
  
  const [users, setUsers] = useState<AdminUserItem[]>([]);
  const [rules, setRules] = useState<FraudRuleItem[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLogDocument[]>([]);
  const [loading, setLoading] = useState(true);

  // Rule Edit Modal
  const [selectedRule, setSelectedRule] = useState<FraudRuleItem | null>(null);
  const [thresholdValue, setThresholdValue] = useState('');
  const [timeWindowSeconds, setTimeWindowSeconds] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [ruleLoading, setRuleLoading] = useState(false);

  const loadAdminData = async () => {
    setLoading(true);
    try {
      const [usersData, rulesData, auditData] = await Promise.all([
        adminApi.getAllUsers(),
        adminApi.getAllFraudRules(),
        adminApi.getAllAuditLogs(),
      ]);
      setUsers(usersData);
      setRules(rulesData);
      setAuditLogs(auditData);
    } catch (err) {
      console.error('Failed to load admin data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAdminData();
  }, []);

  const handleToggleFreeze = async (walletId: number, currentStatus: string) => {
    try {
      if (currentStatus === 'ACTIVE') {
        await adminApi.freezeWallet(walletId);
      } else {
        await adminApi.unfreezeWallet(walletId);
      }
      loadAdminData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update wallet status');
    }
  };

  const handleUpdateRuleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRule) return;
    setRuleLoading(true);

    try {
      await adminApi.updateFraudRule(selectedRule.ruleCode, {
        thresholdValue: parseFloat(thresholdValue),
        timeWindowSeconds: timeWindowSeconds ? parseInt(timeWindowSeconds, 10) : undefined,
        enabled,
      });
      setSelectedRule(null);
      loadAdminData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update fraud rule');
    } finally {
      setRuleLoading(false);
    }
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
      
      {/* Page Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <div style={{
            width: '46px',
            height: '46px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, #6366F1 0%, #06B6D4 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            boxShadow: '0 0 20px rgba(99, 102, 241, 0.4)'
          }}>
            <Settings size={26} />
          </div>
          <div>
            <h2 style={{ fontSize: '1.75rem', fontWeight: 800, letterSpacing: '-0.02em' }}>Admin Command Center</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              User control, fraud engine configuration & system audit stream
            </p>
          </div>
        </div>

        <button onClick={loadAdminData} className="btn btn-secondary">
          <RefreshCw size={16} /> Refresh All
        </button>
      </div>

      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <button
          className={`btn ${activeTab === 'users' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('users')}
        >
          <Users size={16} /> Users & Wallets ({users.length})
        </button>
        <button
          className={`btn ${activeTab === 'rules' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('rules')}
        >
          <ShieldCheck size={16} /> Fraud Rules ({rules.length})
        </button>
        <button
          className={`btn ${activeTab === 'audit' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('audit')}
        >
          <Database size={16} /> MongoDB Audit Logs ({auditLogs.length})
        </button>
      </div>

      {/* Tab 1: Users & Wallets */}
      {activeTab === 'users' && (
        <div className="glass-card animate-fade-in" style={{ padding: '2rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.25rem' }}>Registered System Users</h3>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>Email</th>
                  <th>Name</th>
                  <th>Role</th>
                  <th>Account Number</th>
                  <th>Balance</th>
                  <th>Wallet Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.userId}>
                    <td style={{ fontWeight: 700 }}>#{u.userId}</td>
                    <td style={{ fontWeight: 600 }}>{u.email}</td>
                    <td>{u.firstName} {u.lastName}</td>
                    <td><StatusBadge status={u.role} /></td>
                    <td style={{ fontFamily: 'monospace', color: 'var(--accent-cyan)' }}>{u.accountNumber || 'N/A'}</td>
                    <td style={{ fontWeight: 700 }}>
                      {u.balance !== undefined && u.balance !== null ? `$${u.balance.toFixed(2)}` : 'N/A'}
                    </td>
                    <td>{u.walletStatus ? <StatusBadge status={u.walletStatus} /> : 'N/A'}</td>
                    <td>
                      {u.walletId && (
                        <button
                          onClick={() => handleToggleFreeze(u.walletId!, u.walletStatus || 'ACTIVE')}
                          className={`btn ${u.walletStatus === 'ACTIVE' ? 'btn-danger' : 'btn-success'}`}
                          style={{ padding: '0.35rem 0.65rem', fontSize: '0.75rem' }}
                        >
                          {u.walletStatus === 'ACTIVE' ? (
                            <> <Lock size={13} /> Freeze Wallet </>
                          ) : (
                            <> <Unlock size={13} /> Unfreeze Wallet </>
                          )}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 2: Fraud Rules Management */}
      {activeTab === 'rules' && (
        <div className="glass-card animate-fade-in" style={{ padding: '2rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.25rem' }}>Fraud Rules Engine Configurations</h3>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Rule Name</th>
                  <th>Threshold Value</th>
                  <th>Time Window (sec)</th>
                  <th>State</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {rules.map((r) => (
                  <tr key={r.id}>
                    <td style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--accent-cyan)' }}>{r.ruleCode}</td>
                    <td style={{ fontWeight: 600 }}>{r.ruleName}</td>
                    <td style={{ fontWeight: 800 }}>${r.thresholdValue.toFixed(2)}</td>
                    <td>{r.timeWindowSeconds ? `${r.timeWindowSeconds}s` : 'N/A'}</td>
                    <td>
                      <span className={`badge ${r.enabled ? 'badge-completed' : 'badge-declined'}`}>
                        {r.enabled ? 'ENABLED' : 'DISABLED'}
                      </span>
                    </td>
                    <td>
                      <button
                        onClick={() => {
                          setSelectedRule(r);
                          setThresholdValue(r.thresholdValue.toString());
                          setTimeWindowSeconds(r.timeWindowSeconds ? r.timeWindowSeconds.toString() : '');
                          setEnabled(r.enabled);
                        }}
                        className="btn btn-secondary"
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.75rem' }}
                      >
                        <Edit3 size={13} /> Edit Threshold
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 3: MongoDB Audit Logs Stream */}
      {activeTab === 'audit' && (
        <div className="glass-card animate-fade-in" style={{ padding: '2rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.25rem' }}>MongoDB Asynchronous Domain Audit Events</h3>
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Event ID</th>
                  <th>Event Type</th>
                  <th>Actor</th>
                  <th>Resource</th>
                  <th>Payload Summary</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs.map((log) => (
                  <tr key={log.id}>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.75rem', color: 'var(--text-muted)' }}>{log.eventId}</td>
                    <td>
                      <span className="badge badge-completed">{log.eventType}</span>
                    </td>
                    <td style={{ fontSize: '0.85rem' }}>{log.actorEmail || 'System'}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{log.resourceType}:{log.resourceId}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.75rem', maxWidth: '300px', color: 'var(--accent-cyan)' }}>
                      {JSON.stringify(log.payload)}
                    </td>
                    <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{new Date(log.timestamp).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Edit Fraud Rule Modal */}
      <Modal isOpen={!!selectedRule} onClose={() => setSelectedRule(null)} title={`Edit Fraud Rule: ${selectedRule?.ruleCode}`}>
        {selectedRule && (
          <form onSubmit={handleUpdateRuleSubmit}>
            <div className="form-group">
              <label className="form-label">Threshold Value ($)</label>
              <input
                type="number"
                step="0.01"
                className="form-control"
                value={thresholdValue}
                onChange={(e) => setThresholdValue(e.target.value)}
                required
              />
            </div>

            {selectedRule.timeWindowSeconds !== undefined && (
              <div className="form-group">
                <label className="form-label">Time Window (Seconds)</label>
                <input
                  type="number"
                  className="form-control"
                  value={timeWindowSeconds}
                  onChange={(e) => setTimeWindowSeconds(e.target.value)}
                />
              </div>
            )}

            <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginTop: '1rem' }}>
              <input
                type="checkbox"
                id="ruleEnabled"
                checked={enabled}
                onChange={(e) => setEnabled(e.target.checked)}
                style={{ width: '18px', height: '18px', cursor: 'pointer' }}
              />
              <label htmlFor="ruleEnabled" style={{ fontWeight: 600, cursor: 'pointer' }}>
                Enable this fraud detection rule
              </label>
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', padding: '0.85rem', marginTop: '1.25rem' }} disabled={ruleLoading}>
              {ruleLoading ? 'Updating Rule...' : 'Save Changes'}
            </button>
          </form>
        )}
      </Modal>

    </div>
  );
};
