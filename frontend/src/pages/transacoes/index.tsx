import { Fragment, useCallback, useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { getApiError } from '../../services/api';
import { transacaoService } from '../../services/transacaoService';
import type {
    CategoriaOrcamentaria, Comprovante, ComprovanteCreate, ComprovanteTipo,
    ProjetoOpcao, TipoTransacao, Transacao, TransacaoResumo,
} from '../../types/transacao';
import './transacoes.css';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataBR = (data: string | null) => data ? new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR') : '—';
const tipoLabel: Record<TipoTransacao, string> = { RECEITA: 'Receita', DESPESA: 'Despesa' };
const categoriaLabel: Record<string, string> = { VENDA: 'Venda', EMPRESTIMO: 'Empréstimo', ALIMENTOS: 'Alimentos', OUTROS: 'Outros' };
const tipoComprovanteLabel: Record<ComprovanteTipo, string> = {
    RECIBO: 'Recibo', NOTA_FISCAL: 'Nota fiscal', COMPROVANTE_PAGAMENTO: 'Comprovante de pagamento', OUTRO: 'Outro',
};
const mesAtual = () => { const agora = new Date(); return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}`; };
const inicioDoMes = (mes: string) => mes ? `${mes}-01` : undefined;
const fimDoMes = (mes: string) => {
    if (!mes) return undefined;
    const [ano, numeroMes] = mes.split('-').map(Number);
    const ultimoDia = new Date(ano, numeroMes, 0).getDate();
    return `${ano}-${String(numeroMes).padStart(2, '0')}-${String(ultimoDia).padStart(2, '0')}`;
};

export function Transacoes() {
    const navigate = useNavigate();
    const [transacoes, setTransacoes] = useState<Transacao[]>([]);
    const [categorias, setCategorias] = useState<CategoriaOrcamentaria[]>([]);
    const [projetos, setProjetos] = useState<ProjetoOpcao[]>([]);
    const [resumo, setResumo] = useState<TransacaoResumo | null>(null);
    const [tipoFiltro, setTipoFiltro] = useState<'TODOS' | TipoTransacao>('TODOS');
    const [mesFiltro, setMesFiltro] = useState(mesAtual);
    const [projetoFiltro, setProjetoFiltro] = useState('');
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');
    const [abertaId, setAbertaId] = useState<number | null>(null);
    const [comprovantes, setComprovantes] = useState<Comprovante[]>([]);
    const [comprovantesPrestacao, setComprovantesPrestacao] = useState<Comprovante[]>([]);
    const [vinculoDocumento, setVinculoDocumento] = useState<'TRANSACAO' | 'PRESTACAO'>('TRANSACAO');
    const [carregandoComprovantes, setCarregandoComprovantes] = useState(false);
    const [erroComprovantes, setErroComprovantes] = useState('');
    const [tipoDocumento, setTipoDocumento] = useState<ComprovanteTipo>('COMPROVANTE_PAGAMENTO');
    const [arquivoRef, setArquivoRef] = useState('');
    const [numeroNotaFiscal, setNumeroNotaFiscal] = useState('');
    const [valorDocumento, setValorDocumento] = useState('');
    const [dataDocumento, setDataDocumento] = useState('');
    const [salvandoComprovante, setSalvandoComprovante] = useState(false);

    const carregar = useCallback(async () => {
        setCarregando(true); setErro('');
        const dataInicio = inicioDoMes(mesFiltro);
        const dataFim = fimDoMes(mesFiltro);
        try {
            const [lista, listaCategorias, listaProjetos, dadosResumo] = await Promise.all([
                transacaoService.listar({
                    ...(tipoFiltro === 'TODOS' ? {} : { tipo: tipoFiltro }),
                    ...(projetoFiltro ? { projetoId: Number(projetoFiltro) } : {}),
                    ...(dataInicio ? { dataInicio } : {}), ...(dataFim ? { dataFim } : {}),
                }),
                transacaoService.listarCategoriasOrcamentarias(),
                transacaoService.listarProjetosAtivos(),
                dataInicio && dataFim ? transacaoService.resumir(dataInicio, dataFim) : Promise.resolve(null),
            ]);
            setTransacoes(lista); setCategorias(listaCategorias); setProjetos(listaProjetos); setResumo(dadosResumo);
        } catch (error) { setErro(getApiError(error, 'Não foi possível carregar as transações.')); }
        finally { setCarregando(false); }
    }, [mesFiltro, tipoFiltro, projetoFiltro]);

    useEffect(() => { void carregar(); }, [carregar]);

    const nomeCategoria = (id: number | null) => id == null ? '—' : categorias.find((categoria) => categoria.id === id)?.nome ?? `Categoria #${id}`;
    const saldoPeriodo = resumo ? Number(resumo.resultado) : 0;

    async function alternarComprovantes(transacaoId: number) {
        if (abertaId === transacaoId) { setAbertaId(null); return; }
        setAbertaId(transacaoId); setComprovantes([]); setComprovantesPrestacao([]); setErroComprovantes(''); setCarregandoComprovantes(true);
        const transacao = transacoes.find((item) => item.id === transacaoId);
        try {
            const [docsTransacao, docsPrestacao] = await Promise.all([
                transacaoService.listarComprovantes(transacaoId),
                transacao?.prestacaoId ? transacaoService.listarComprovantesPrestacao(transacao.prestacaoId) : Promise.resolve([]),
            ]);
            setComprovantes(docsTransacao); setComprovantesPrestacao(docsPrestacao);
            setVinculoDocumento(transacao?.prestacaoId ? 'PRESTACAO' : 'TRANSACAO');
        } catch (error) { setErroComprovantes(getApiError(error, 'Não foi possível carregar os comprovantes.')); }
        finally { setCarregandoComprovantes(false); }
    }

    async function adicionarComprovante(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (abertaId == null || !arquivoRef.trim()) return;
        const valor = valorDocumento.trim() ? Number(valorDocumento.replace(/\./g, '').replace(',', '.')) : null;
        if (valor !== null && (!Number.isFinite(valor) || valor <= 0)) { setErroComprovantes('Informe um valor válido para o documento.'); return; }
        const payload: ComprovanteCreate = {
            tipo: tipoDocumento, arquivoRef: arquivoRef.trim(),
            numeroNotaFiscal: numeroNotaFiscal.trim() || null, valor, dataDocumento: dataDocumento || null,
        };
        setSalvandoComprovante(true); setErroComprovantes('');
        try {
            const transacaoAtual = transacoes.find((item) => item.id === abertaId);
            if (vinculoDocumento === 'PRESTACAO' && transacaoAtual?.prestacaoId) {
                await transacaoService.adicionarComprovantePrestacao(transacaoAtual.prestacaoId, payload);
                setComprovantesPrestacao(await transacaoService.listarComprovantesPrestacao(transacaoAtual.prestacaoId));
            } else {
                await transacaoService.adicionarComprovante(abertaId, payload);
                setComprovantes(await transacaoService.listarComprovantes(abertaId));
            }
            setArquivoRef(''); setNumeroNotaFiscal(''); setValorDocumento(''); setDataDocumento('');
        } catch (error) { setErroComprovantes(getApiError(error, 'Não foi possível adicionar o comprovante.')); }
        finally { setSalvandoComprovante(false); }
    }

    return (
        <div className="transactions-page">
            <header className="transactions-header">
                <div><h1>Receitas e despesas</h1><p className="transactions-breadcrumb">Hut8&nbsp; / &nbsp;Financeiro&nbsp; / &nbsp;Transações</p></div>
                <button className="transactions-new-button" type="button" onClick={() => navigate('/app/transacoes/nova')}>+ Nova transação</button>
            </header>
            <main className="transactions-content">
                <section className="transactions-summary" aria-label="Resumo financeiro do período">
                    <article className="transaction-summary-card"><span>Receitas</span><strong className="summary-income">{moeda.format(Number(resumo?.receitas ?? 0))}</strong></article>
                    <article className="transaction-summary-card"><span>Despesas</span><strong className="summary-expense">{moeda.format(Number(resumo?.despesas ?? 0))}</strong></article>
                    <article className="transaction-summary-card"><span>Saldo do período</span><strong className={saldoPeriodo >= 0 ? 'summary-positive' : 'summary-negative'}>{moeda.format(saldoPeriodo)}</strong></article>
                </section>
                <section className="transactions-history" aria-label="Histórico de transações">
                    <div className="transactions-history-heading">
                        <h2>Histórico de transações</h2>
                        <div className="transactions-filters">
                            <label><span>Tipo</span><select value={tipoFiltro} onChange={(event) => setTipoFiltro(event.target.value as 'TODOS' | TipoTransacao)}><option value="TODOS">Todos</option><option value="RECEITA">Receitas</option><option value="DESPESA">Despesas</option></select></label>
                            <label><span>Projeto</span><select value={projetoFiltro} onChange={(event) => setProjetoFiltro(event.target.value)}><option value="">Todos</option>{projetos.map((projeto) => <option key={projeto.id} value={projeto.id}>{projeto.nome}</option>)}</select></label>
                            <label><span>Período</span><input type="month" value={mesFiltro} onChange={(event) => setMesFiltro(event.target.value)} /></label>
                        </div>
                    </div>
                    {erro && <div className="transactions-feedback transactions-error" role="alert">{erro}</div>}
                    {carregando ? <div className="transactions-feedback" aria-live="polite">Carregando transações...</div>
                        : erro ? <div className="transactions-empty">Não foi possível apresentar o histórico.</div>
                            : transacoes.length === 0 ? <div className="transactions-empty">Nenhuma transação encontrada para os filtros selecionados.</div>
                                : <div className="transactions-table-wrap"><table className="transactions-table">
                                    <thead><tr><th>Descrição</th><th>Tipo</th><th>Categoria</th><th>Data</th><th>Valor</th><th>Comprovantes</th></tr></thead>
                                    <tbody>{transacoes.map((transacao) => <Fragment key={transacao.id}>
                                        <tr key={transacao.id}>
                                            <td><strong>{transacao.descricao}</strong>{transacao.projetoId && <small>{projetos.find((p) => p.id === transacao.projetoId)?.nome ?? `Projeto #${transacao.projetoId}`}</small>}{transacao.prestacaoId && <small>Prestação #{transacao.prestacaoId}</small>}</td>
                                            <td><span className={`transaction-type-badge ${transacao.tipo.toLowerCase()}`}>{tipoLabel[transacao.tipo]}</span></td>
                                            <td><span>{categoriaLabel[transacao.categoriaTransacao] ?? transacao.categoriaTransacao}</span>{transacao.tipo === 'DESPESA' && <small>{nomeCategoria(transacao.categoriaOrcamentariaId)}</small>}{transacao.subverbaId && <small>Subverba #{transacao.subverbaId}</small>}</td>
                                            <td>{dataBR(transacao.dataMovimento)}</td>
                                            <td className={`transaction-value ${transacao.tipo.toLowerCase()}`}>{transacao.tipo === 'RECEITA' ? '+ ' : '− '}{moeda.format(Number(transacao.valor))}</td>
                                            <td><button type="button" className="transaction-link-button" onClick={() => void alternarComprovantes(transacao.id)}>{abertaId === transacao.id ? 'Fechar' : 'Ver / adicionar'}</button></td>
                                        </tr>
                                        {abertaId === transacao.id && <tr key={`comprovantes-${transacao.id}`}><td colSpan={6}>
                                            <section className="transaction-attachments">
                                                <h3>Comprovantes da transação #{transacao.id}</h3>
                                                {transacao.prestacaoId && <label className="transaction-field">Vincular comprovante a<select value={vinculoDocumento} onChange={(event) => setVinculoDocumento(event.target.value as 'TRANSACAO' | 'PRESTACAO')}><option value="TRANSACAO">Transação #{transacao.id}</option><option value="PRESTACAO">Prestação #{transacao.prestacaoId}</option></select></label>}
                                                {carregandoComprovantes ? <p>Carregando comprovantes...</p> : erroComprovantes ? <p className="transactions-error">{erroComprovantes}</p> : (vinculoDocumento === 'PRESTACAO' ? comprovantesPrestacao : comprovantes).length === 0 ? <p>Nenhum comprovante cadastrado para este vínculo.</p> : <ul>{(vinculoDocumento === 'PRESTACAO' ? comprovantesPrestacao : comprovantes).map((item) => <li key={item.id}><span><strong>{tipoComprovanteLabel[item.tipo]}</strong>{item.numeroNotaFiscal ? ` · NF ${item.numeroNotaFiscal}` : ''}{item.dataDocumento ? ` · ${dataBR(item.dataDocumento)}` : ''}{item.valor != null ? ` · ${moeda.format(Number(item.valor))}` : ''}</span>{/^https?:\/\//i.test(item.arquivoRef) ? <a href={item.arquivoRef} target="_blank" rel="noreferrer">Abrir anexo</a> : <code>{item.arquivoRef}</code>}</li>)}</ul>}
                                                <form className="transaction-attachment-form" onSubmit={adicionarComprovante}>
                                                    <label>Tipo<select value={tipoDocumento} onChange={(event) => setTipoDocumento(event.target.value as ComprovanteTipo)}>{Object.entries(tipoComprovanteLabel).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                                                    <label>Referência do arquivo (URL ou caminho)<input required maxLength={4000} value={arquivoRef} onChange={(event) => setArquivoRef(event.target.value)} placeholder="https://... ou caminho do arquivo" /></label>
                                                    <label>Nº nota fiscal<input maxLength={60} value={numeroNotaFiscal} onChange={(event) => setNumeroNotaFiscal(event.target.value)} /></label>
                                                    <label>Valor do documento<input inputMode="decimal" value={valorDocumento} onChange={(event) => setValorDocumento(event.target.value)} placeholder="Opcional" /></label>
                                                    <label>Data do documento<input type="date" value={dataDocumento} onChange={(event) => setDataDocumento(event.target.value)} /></label>
                                                    <button type="submit" className="transactions-new-button" disabled={salvandoComprovante}>{salvandoComprovante ? 'Salvando...' : 'Adicionar referência'}</button>
                                                </form>
                                                <p className="transaction-attachment-note">A API atual armazena a referência do arquivo, não o conteúdo binário. O arquivo deve estar previamente disponível no endereço ou caminho informado.</p>
                                            </section>
                                        </td></tr>}
                                    </Fragment>)}</tbody>
                                </table></div>}
                    {!carregando && !erro && <p className="transactions-count">{transacoes.length} movimentação(ões) exibida(s){resumo ? ` · ${resumo.quantidade} no período` : ''}</p>}
                </section>
            </main>
        </div>
    );
}

export { NovaTransacao } from './nova';
