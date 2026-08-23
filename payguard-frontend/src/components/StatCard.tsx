import React from 'react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  accentColor?: string;
}

export const StatCard: React.FC<StatCardProps> = ({ title, value, subtitle, icon, accentColor = 'var(--primary)' }) => {
  return (
    <div className="glass-card" style={{ padding: '1.25rem 1.5rem', position: 'relative', overflow: 'hidden' }}>
      <div style={{
        position: 'absolute',
        top: '-15px',
        right: '-15px',
        width: '80px',
        height: '80px',
        borderRadius: '50%',
        background: accentColor,
        opacity: 0.1,
        filter: 'blur(20px)',
      }} />

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
        <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
          {title}
        </span>
        <div style={{
          width: '36px',
          height: '36px',
          borderRadius: '10px',
          background: 'rgba(255, 255, 255, 0.05)',
          border: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: accentColor,
        }}>
          {icon}
        </div>
      </div>

      <div style={{ fontSize: '1.75rem', fontWeight: 800, letterSpacing: '-0.03em' }}>
        {value}
      </div>

      {subtitle && (
        <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '0.25rem' }}>
          {subtitle}
        </div>
      )}
    </div>
  );
};
