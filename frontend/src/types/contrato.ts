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
