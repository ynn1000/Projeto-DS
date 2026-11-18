export type StatusOrcamento = 'PENDENTE' | 'EM_ANALISE' | 'APROVADO' | 'REJEITADO';

export interface OrcamentoPayload {
    categoriaId: number;
    titulo: string;
    descricao: string;
    valorSolicitado: number;
    arquivoRef: string | null;
}

export interface Orcamento {
    id: number;
    solicitanteId: string;
    solicitanteNome: string;
    categoriaId: number;
    categoriaNome: string;
    titulo: string;
    descricao: string;
    valorSolicitado: number;
    arquivoRef: string | null;
    status: StatusOrcamento;
    avaliadoPorId: string | null;
    justificativaDecisao: string | null;
    dataDecisao: string | null;
    criadoEm: string;
}

export interface OrcamentoDecisaoPayload {
    status: StatusOrcamento;
    justificativa?: string;
}
