import type { StatusOrcamento } from '../../types/orcamento';

export const rotulosStatus: Record<StatusOrcamento, string> = {
    PENDENTE: 'Pendente',
    EM_ANALISE: 'Em análise',
    APROVADO: 'Aprovado',
    REJEITADO: 'Rejeitado',
};

export const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const data = new Intl.DateTimeFormat('pt-BR');
