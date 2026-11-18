import api from './api';
import type { RoleUsuario } from '../types/usuario';
import type { CategoriaOrcamentaria } from '../types/transacao';
import type { Orcamento, OrcamentoDecisaoPayload, OrcamentoPayload, StatusOrcamento } from '../types/orcamento';

export const orcamentoService = {
    async consultarPerfil(): Promise<RoleUsuario> {
        const { data } = await api.get<RoleUsuario>('/solicitar_orcamento/perfil');
        return data;
    },
    async listarCategorias(): Promise<CategoriaOrcamentaria[]> {
        const { data } = await api.get<CategoriaOrcamentaria[]>('/solicitar_orcamento/categorias');
        return data;
    },
    async solicitar(payload: OrcamentoPayload): Promise<Orcamento> {
        const { data } = await api.post<Orcamento>('/solicitar_orcamento', payload);
        return data;
    },
    async listarMinhas(): Promise<Orcamento[]> {
        const { data } = await api.get<Orcamento[]>('/solicitar_orcamento/minhas');
        return data;
    },
    async listarTodas(status?: StatusOrcamento): Promise<Orcamento[]> {
        const { data } = await api.get<Orcamento[]>('/solicitar_orcamento/todas', { params: status ? { status } : {} });
        return data;
    },
    async consultar(id: number): Promise<Orcamento> {
        const { data } = await api.get<Orcamento>(`/solicitar_orcamento/${id}`);
        return data;
    },
    async alterarStatus(id: number, payload: OrcamentoDecisaoPayload): Promise<Orcamento> {
        const { data } = await api.patch<Orcamento>(`/solicitar_orcamento/${id}/status`, payload);
        return data;
    },
};
