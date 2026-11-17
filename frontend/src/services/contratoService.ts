import api from './api';
import type { Contrato, ContratoPayload, FiltrosContrato, StatusContrato } from '../types/contrato';

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
    async excluir(id: number) {
        await api.delete(`${RESOURCE}/${id}`);
    },
};
