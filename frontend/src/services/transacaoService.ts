import api from './api';
import type {
    CategoriaOrcamentaria, Comprovante, ComprovanteCreate, PeriodoOrcamento,
    PrestacaoOpcao, ProjetoOpcao, Subverba, Transacao, TransacaoCreate,
    TransacaoFiltros, TransacaoResumo,
} from '../types/transacao';

export const transacaoService = {
    async listar(filtros: TransacaoFiltros = {}): Promise<Transacao[]> {
        const params = Object.fromEntries(Object.entries(filtros).filter(([, value]) => value !== undefined && value !== ''));
        const response = await api.get<Transacao[]>('/transacoes', { params });
        return response.data;
    },
    async resumir(dataInicio: string, dataFim: string): Promise<TransacaoResumo> {
        const response = await api.get<TransacaoResumo>('/transacoes/resumo', { params: { dataInicio, dataFim } });
        return response.data;
    },
    async criar(payload: TransacaoCreate): Promise<Transacao> {
        const response = await api.post<Transacao>('/transacoes', payload);
        return response.data;
    },
    async listarProjetosAtivos(): Promise<ProjetoOpcao[]> {
        const response = await api.get<ProjetoOpcao[]>('/transacoes/opcoes/projetos');
        return response.data;
    },
    async listarPrestacoesPendentes(): Promise<PrestacaoOpcao[]> {
        const response = await api.get<PrestacaoOpcao[]>('/transacoes/opcoes/prestacoes-pendentes');
        return response.data;
    },
    async listarCategoriasOrcamentarias(): Promise<CategoriaOrcamentaria[]> {
        const response = await api.get<CategoriaOrcamentaria[]>('/categorias-orcamentarias');
        return response.data;
    },
    async listarPeriodosOrcamentoAtivos(): Promise<PeriodoOrcamento[]> {
        const response = await api.get<PeriodoOrcamento[]>('/transacoes/opcoes/periodos-orcamento-ativos');
        return response.data;
    },
    async listarSubverbas(periodoId: number, categoriaId: number): Promise<Subverba[]> {
        const response = await api.get<Subverba[]>(`/orcamentos/${periodoId}/verbas/${categoriaId}/subverbas`);
        return response.data;
    },
    async listarComprovantes(transacaoId: number): Promise<Comprovante[]> {
        const response = await api.get<Comprovante[]>(`/transacoes/${transacaoId}/comprovantes`);
        return response.data;
    },
    async adicionarComprovante(transacaoId: number, payload: ComprovanteCreate): Promise<Comprovante> {
        const response = await api.post<Comprovante>(`/transacoes/${transacaoId}/comprovantes`, payload);
        return response.data;
    },
    async listarComprovantesPrestacao(prestacaoId: number): Promise<Comprovante[]> {
        const response = await api.get<Comprovante[]>(`/transacoes/prestacoes/${prestacaoId}/comprovantes`);
        return response.data;
    },
    async adicionarComprovantePrestacao(prestacaoId: number, payload: ComprovanteCreate): Promise<Comprovante> {
        const response = await api.post<Comprovante>(`/transacoes/prestacoes/${prestacaoId}/comprovantes`, payload);
        return response.data;
    },
};
