import axios from 'axios';

const TOKEN_KEY = 'leaveflow.token';
const USER_KEY = 'leaveflow.user';

export const session = {
  get token() {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  get user() {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },
  save(token, user) {
    try {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } catch {
      /* storage unavailable: session lasts for this page only */
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    } catch {
      /* ignore */
    }
  },
};

let memoryToken = session.token;
let onUnauthorized = () => {};

export function setAuthToken(token) {
  memoryToken = token;
}

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
});

client.interceptors.request.use((config) => {
  if (memoryToken) {
    config.headers.Authorization = `Bearer ${memoryToken}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLogin = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !isLogin) {
      onUnauthorized();
    }
    return Promise.reject(error);
  },
);

/** Turns an Axios error into a human-readable message (uses the backend's ApiError body). */
export function errorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const body = error?.response?.data;
  if (body?.fieldErrors?.length) {
    return body.fieldErrors.map((f) => f.message).join(' · ');
  }
  if (body?.message) return body.message;
  if (error?.code === 'ECONNABORTED') return 'The server took too long to respond.';
  if (error?.message === 'Network Error') return 'Cannot reach the server. Is the backend running?';
  return fallback;
}

export function fieldErrors(error) {
  const list = error?.response?.data?.fieldErrors || [];
  return Object.fromEntries(list.map((f) => [f.field, f.message]));
}

export default client;
