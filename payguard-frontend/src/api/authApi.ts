import api from './axios';

export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  status: string;
  accountNumber: string;
  createdAt: string;
}

export interface AuthResponse {
  userId: number;
  email: string;
  role: string;
  message: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export const authApi = {
  register: async (payload: RegisterPayload): Promise<UserResponse> => {
    const res = await api.post<UserResponse>('/api/v1/auth/register', payload);
    return res.data;
  },

  login: async (payload: LoginPayload): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>('/api/v1/auth/login', payload);
    return res.data;
  },

  logout: async (): Promise<void> => {
    await api.post('/api/v1/auth/logout');
  },
};
