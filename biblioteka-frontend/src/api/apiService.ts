import axios, { AxiosError } from "axios";

const baseURL =
  import.meta.env.VITE_API_URL?.replace(/\/+$/, "") || "http://localhost:4000/api";

export const api = axios.create({ baseURL });

// --- Token utils (drži sve na jednom mestu)
const TOKEN_KEY = "token";

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

// --- Request interceptor: dodaj Bearer token ako postoji
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// --- (opciono) Response interceptor: auto-logout na 401
api.interceptors.response.use(
  (res) => res,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // npr. obriši token i preusmeri na login
      setToken(null);
      // window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default api;
