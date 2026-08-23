import axios from 'axios';

const api = axios.create({
  baseURL: '', // Uses Vite proxy configuration (/api)
  withCredentials: true, // Pass HTTP-Only JWT cookies
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface ApiErrorResponse {
  status: number;
  error: String;
  message: string;
  timestamp: string;
  validationErrors?: Record<string, string>;
}

export default api;
