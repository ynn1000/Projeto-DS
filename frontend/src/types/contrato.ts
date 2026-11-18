export type StatusContrato = 'RASCUNHO' | 'ATIVO' | 'ENCERRADO' | 'RESCINDIDO';

export interface Contrato {
    id: number;
    projetoId: number;
    clienteNome: string;
    arquivoRef: string | null;
    valorTotal: number;
    dataInicio: string;
    dataFim: string;
    status: StatusContrato;
}

export interface ContratoPayload {
    projetoId: number;
    clienteNome: string;
    arquivoRef: string | null;
    valorTotal: number;
    dataInicio: string;
    dataFim: string;
}

export interface FiltrosContrato {
    projetoId?: number;
    clienteNome?: string;
    status?: StatusContrato;
}

export type StatusPrestacao = 'PENDENTE' | 'PAGA' | 'CANCELADA';

export interface PrestacaoContrato {
    id: number;
    contratoId: number;
    numero: number;
    valor: number;
    dataVencimento: string;
    status: StatusPrestacao;
    transacaoId: number | null;
    quantidadeComprovantes: number;
}

export interface PrestacaoPayload {
    numero: number;
    valor: number;
    dataVencimento: string;
}
