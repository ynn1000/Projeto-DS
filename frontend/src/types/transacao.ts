export type TipoTransacao = 'RECEITA' | 'DESPESA';
export type CategoriaTransacao = 'VENDA' | 'EMPRESTIMO' | 'ALIMENTOS' | 'OUTROS';
export type ComprovanteTipo = 'RECIBO' | 'NOTA_FISCAL' | 'COMPROVANTE_PAGAMENTO' | 'OUTRO';

export interface Transacao {
    id: number;
    descricao: string;
    tipo: TipoTransacao;
    categoriaTransacao: CategoriaTransacao;
    valor: number;
    dataMovimento: string;
    categoriaOrcamentariaId: number | null;
    subverbaId: number | null;
    projetoId: number | null;
    prestacaoId: number | null;
    registradoPorId: number | null;
    criadoEm: string;
}

export interface TransacaoCreate {
    descricao: string;
    tipo: TipoTransacao;
    categoriaTransacao: CategoriaTransacao;
    valor: number;
    dataMovimento: string;
    categoriaOrcamentariaId: number | null;
    subverbaId: number | null;
    projetoId: number | null;
    prestacaoId: number | null;
}

export interface CategoriaOrcamentaria {
    id: number;
    nome: string;
    descricao: string | null;
}

export interface PeriodoOrcamento {
    id: number;
    nome: string;
    dataInicio: string;
    dataFim: string;
}

export interface Subverba {
    id: number;
    orcamentoPeriodoId: number;
    categoriaOrcamentariaId: number;
    nome: string;
    valorReservado: number;
    valorUtilizado: number;
    saldoSubverba: number;
}

export interface ProjetoOpcao { id: number; nome: string }
export interface PrestacaoOpcao {
    id: number;
    contratoId: number;
    projetoId: number;
    projetoNome: string;
    clienteNome: string;
    numero: number;
    valor: number;
    dataVencimento: string;
}

export interface Comprovante {
    id: number;
    transacaoId: number | null;
    prestacaoId: number | null;
    tipo: ComprovanteTipo;
    arquivoRef: string;
    numeroNotaFiscal: string | null;
    valor: number | null;
    dataDocumento: string | null;
    criadoEm: string;
}

export interface ComprovanteCreate {
    tipo: ComprovanteTipo;
    arquivoRef: string;
    numeroNotaFiscal: string | null;
    valor: number | null;
    dataDocumento: string | null;
}

export interface TransacaoResumo {
    dataInicio: string;
    dataFim: string;
    receitas: number;
    despesas: number;
    resultado: number;
    quantidade: number;
}

export interface TransacaoFiltros {
    tipo?: TipoTransacao;
    projetoId?: number;
    dataInicio?: string;
    dataFim?: string;
}
