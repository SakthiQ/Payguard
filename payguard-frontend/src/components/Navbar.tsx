import React from 'react';
import { useAuth } from '../context/AuthContext';
import { StatusBadge } from './StatusBadge';
import { Shield, LogOut, Wallet, UserCheck, Settings, PieChart } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <header style={{
      borderBottom: '1px solid var(--border-color)',
      background: 'rgba(11, 15, 25, 0.85)',
      backdropFilter: 'blur(16px)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>
      <div style={{
        maxWidth: '1280px',
        margin: '0 auto',
        padding: '0.85rem 1.5rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}>
        {/* Brand */}
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', textDecoration: 'none', color: 'inherit' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: '10px',
            background: 'linear-gradient(135deg, #6366F1 0%, #06B6D4 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 15px rgba(99, 102, 241, 0.4)'
          }}>
            <Shield color="white" size={22} />
          </div>
          <div>
            <span style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em', background: 'linear-gradient(90deg, #F9FAFB, #9CA3AF)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              PayGuard
            </span>
            <span style={{ fontSize: '0.7rem', display: 'block', color: 'var(--accent-cyan)', fontWeight: 700, marginTop: '-3px' }}>
              FINTECH BACKEND PROTOCOL
            </span>
          </div>
        </Link>

        {/* User Info & Quick Links */}
        {user && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
            {/* Quick Navigation based on role */}
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Link to="/dashboard" className="btn btn-secondary" style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem' }}>
                <Wallet size={15} /> Customer
              </Link>
              <Link to="/expense-tracker" className="btn btn-secondary" style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem', color: '#D8B4FE', borderColor: 'rgba(168, 85, 247, 0.4)' }}>
                <PieChart size={15} /> Expense Tracker
              </Link>
              {(user.role === 'ROLE_FRAUD_ANALYST' || user.role === 'ROLE_ADMIN') && (
                <Link to="/fraud-analyst" className="btn btn-secondary" style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem' }}>
                  <UserCheck size={15} /> Fraud Queue
                </Link>
              )}
              {user.role === 'ROLE_ADMIN' && (
                <Link to="/admin" className="btn btn-secondary" style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem' }}>
                  <Settings size={15} /> Admin
                </Link>
              )}
            </div>

            <div style={{ height: '24px', width: '1px', background: 'var(--border-color)' }} />

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>{user.email}</div>
                <StatusBadge status={user.role} />
              </div>

              <button onClick={handleLogout} className="btn btn-secondary" title="Logout" style={{ padding: '0.5rem' }}>
                <LogOut size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </header>
  );
};
