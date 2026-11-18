import api from './api';
import type { Contrato, ContratoPayload, FiltrosContrato, PrestacaoContrato, PrestacaoPayload, StatusContrato } from '../types/contrato';

const RESOURCE = '/contratos';

export const contratoService = {
    async listar(filtros: FiltrosContrato = {}) {
        const params = Object.fromEntries(Object.entries(filtros).filter(([, value]) => value !== undefined && value !== ''));
        const response = await api.get<Contrato[]>(RESOURCE, { params });
        return response.data;
    },
    async buscarPorId(id: number) {
        const response = await api.get<Contrato>(`${RESOURCE}/${id}`);
        return response.data;
    },
    async criar(payload: ContratoPayload) {
        const response = await api.post<Contrato>(RESOURCE, payload);
        return response.data;
    },
    async atualizar(id: number, payload: ContratoPayload) {
        const response = await api.put<Contrato>(`${RESOURCE}/${id}`, payload);
        return response.data;
    },
    async atualizarStatus(id: number, status: StatusContrato) {
        const response = await api.patch<Contrato>(`${RESOURCE}/${id}/status`, { status });
        return response.data;
    },
    async listarPrestacoes(id: number): Promise<PrestacaoContrato[]> {
        const response = await api.get<PrestacaoContrato[]>(`${RESOURCE}/${id}/prestacoes`);
        return response.data;
    },
    async criarPrestacao(id: number, payload: PrestacaoPayload): Promise<PrestacaoContrato> {
        const response = await api.post<PrestacaoContrato>(`${RESOURCE}/${id}/prestacoes`, payload);
        return response.data;
    },
    async atualizarPrestacao(id: number, prestacaoId: number, payload: PrestacaoPayload): Promise<PrestacaoContrato> {
        const response = await api.put<PrestacaoContrato>(`${RESOURCE}/${id}/prestacoes/${prestacaoId}`, payload);
        return response.data;
    },
    async cancelarPrestacao(id: number, prestacaoId: number) {
        await api.patch(`${RESOURCE}/${id}/prestacoes/${prestacaoId}/cancelamento`);
    },
    async excluir(id: number) {
        await api.delete(`${RESOURCE}/${id}`);
    },
};
