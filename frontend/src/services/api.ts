import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
    headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
    // O login é público; nunca envie um token antigo nessa requisição.
    if (config.url !== '/auth/login') {
        const token = localStorage.getItem('session');
        if (token) config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
        if (axios.isAxiosError(error) && error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
            localStorage.removeItem('session');
            if (window.location.pathname !== '/auth/login') window.location.assign('/auth/login');
        }
        return Promise.reject(error);
    },
);

export function getApiError(error: unknown, fallback = 'Não foi possível concluir a operação.') {
    if (axios.isAxiosError(error)) {
        if (!error.response) return 'Não foi possível conectar à API. Verifique se o backend está em execução.';
        const data = error.response.data as { detail?: string; message?: string; title?: string } | undefined;
        if (error.response.status === 401) return 'Sua sessão expirou ou não é válida. Entre novamente.';
        if (error.response.status === 403) return data?.detail || data?.message || 'Seu usuário não tem autorização para realizar esta operação.';
        return data?.detail || data?.message || data?.title || fallback;
    }
    return fallback;
}

export default api;
