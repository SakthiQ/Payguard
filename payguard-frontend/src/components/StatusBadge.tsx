import React from 'react';
import { CheckCircle2, AlertTriangle, XCircle, ShieldAlert, Lock, ShieldCheck } from 'lucide-react';

interface StatusBadgeProps {
  status: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const normalized = status.toUpperCase();

  const getStyleAndIcon = () => {
    switch (normalized) {
      case 'COMPLETED':
      case 'ACTIVE':
      case 'APPROVED':
        return {
          className: 'badge-completed',
          icon: <CheckCircle2 size={13} />,
        };
      case 'FLAGGED':
      case 'UNDER_REVIEW':
        return {
          className: 'badge-flagged',
          icon: <AlertTriangle size={13} />,
        };
      case 'DECLINED':
      case 'FAILED':
        return {
          className: 'badge-declined',
          icon: <XCircle size={13} />,
        };
      case 'FROZEN':
        return {
          className: 'badge-frozen',
          icon: <Lock size={13} />,
        };
      case 'ROLE_ADMIN':
        return {
          className: 'badge-completed',
          icon: <ShieldCheck size={13} />,
        };
      case 'ROLE_FRAUD_ANALYST':
        return {
          className: 'badge-flagged',
          icon: <ShieldAlert size={13} />,
        };
      default:
        return {
          className: 'badge-secondary',
          icon: null,
        };
    }
  };

  const { className, icon } = getStyleAndIcon();

  return (
    <span className={`badge ${className}`}>
      {icon}
      {status.replace('ROLE_', '').replace('_', ' ')}
    </span>
  );
};
