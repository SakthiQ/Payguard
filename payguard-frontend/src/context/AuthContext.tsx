import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';
import type { AuthResponse, RegisterPayload, LoginPayload } from '../api/authApi';

interface UserState {
  userId: number;
  email: string;
  role: string;
}

interface AuthContextType {
  user: UserState | null;
  loading: boolean;
  login: (payload: LoginPayload) => Promise<AuthResponse>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserState | null>(() => {
    const saved = localStorage.getItem('payguard_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) {
      localStorage.setItem('payguard_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('payguard_user');
    }
  }, [user]);

  const login = async (payload: LoginPayload): Promise<AuthResponse> => {
    setLoading(true);
    try {
      const res = await authApi.login(payload);
      const userObj = { userId: res.userId, email: res.email, role: res.role };
      setUser(userObj);
      return res;
    } finally {
      setLoading(false);
    }
  };

  const register = async (payload: RegisterPayload): Promise<void> => {
    setLoading(true);
    try {
      await authApi.register(payload);
    } finally {
      setLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await authApi.logout();
    } catch {
      // Ignore errors on logout
    } finally {
      setUser(null);
      localStorage.removeItem('payguard_user');
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
