import api from './api';

export const categoriasTransacao = {
    VENDA: 'Venda',
    EMPRESTIMO: 'Empréstimo',
    ALIMENTOS: 'Alimentos',
    OUTROS: 'Outros',
} as const;

export type TipoTransacao = 'RECEITA' | 'DESPESA';
export type CategoriaTransacao = keyof typeof categoriasTransacao;

export interface Lancamento {
    descricao: string;
    tipo: TipoTransacao;
    categoriaTransacao: CategoriaTransacao;
    valor: number;
    dataMovimento: string;
    categoriaOrcamentariaId: number | null;
    subverbaId: number | null;
}

export interface Caixa {
    saldoAtual: number;
}

export interface Recebimentos {
    receitaPrevista: number;
    proximosVencimentos: {
        id: number;
        contratoId: number;
        clienteNome: string;
        numero: number;
        dataVencimento: string;
        valor: number;
    }[];
}

export interface OpcoesLancamento {
    verbas: { id: number; nome: string }[];
    subverbas: { id: number; nome: string; categoriaOrcamentariaId: number; periodo: string }[];
}

export const dashboardService = {
    async caixa(signal: AbortSignal) {
        return (await api.get<Caixa>('/caixa', { signal })).data;
    },
    async transacoes(signal: AbortSignal) {
        return (await api.get<Lancamento[]>('/transacoes', { signal })).data;
    },
    async recebimentos(signal: AbortSignal) {
        return (await api.get<Recebimentos>('/dashboard/recebimentos', { signal })).data;
    },
    async opcoes(dataMovimento: string, signal: AbortSignal) {
        return (await api.get<OpcoesLancamento>('/dashboard/opcoes-lancamento', {
            params: { dataMovimento }, signal,
        })).data;
    },
    async registrar(lancamento: Lancamento) {
        return (await api.post<Lancamento>('/transacoes', lancamento)).data;
    },
};
