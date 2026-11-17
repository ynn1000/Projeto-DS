import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
    headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('session');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

export function getApiError(error: unknown, fallback = 'Não foi possível concluir a operação.') {
    if (axios.isAxiosError(error)) {
        if (!error.response) return 'Não foi possível conectar à API. Verifique se o backend está em execução.';
        const data = error.response.data as { detail?: string; message?: string; title?: string } | undefined;
        if (error.response.status === 401) return 'Sua sessão expirou ou não é válida. Entre novamente.';
        if (error.response.status === 403) return 'Seu usuário não tem autorização para realizar esta operação.';
        return data?.detail || data?.message || data?.title || fallback;
    }
    return fallback;
}

export default api;
