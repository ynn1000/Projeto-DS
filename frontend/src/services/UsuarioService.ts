import api from './api';
import type { Usuario, UsuarioPayload, UsuarioUpdatePayload } from '../types/usuario';

export const usuarioService = {
    async listar(): Promise<Usuario[]> {
        const { data } = await api.get<Usuario[]>('/usuarios');
        return data;
    },

    async buscarPorId(id: string): Promise<Usuario> {
        const { data } = await api.get<Usuario>(`/usuarios/${id}`);
        return data;
    },

    async criar(payload: UsuarioPayload): Promise<Usuario> {
        const { data } = await api.post<Usuario>('/auth/register', payload);
        return data;
    },

    async atualizar(id: string, payload: UsuarioUpdatePayload): Promise<Usuario> {
        const { data } = await api.patch<Usuario>(`/usuarios/${id}`, payload);
        return data;
    },

    async excluir(id: string): Promise<void> {
        await api.delete(`/usuarios/${id}`);
    },
};