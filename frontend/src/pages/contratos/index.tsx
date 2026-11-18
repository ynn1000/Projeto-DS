import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { contratoService } from '../../services/contratoService';
import { getApiError } from '../../services/api';
import type { Contrato, ContratoPayload, PrestacaoContrato, PrestacaoPayload, StatusContrato } from '../../types/contrato';
import { transacaoService } from '../../services/transacaoService';
import type { Comprovante, ComprovanteCreate, ComprovanteTipo } from '../../types/transacao';
import './contratos.css';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const formatarValorInput = (valor: number) => (valor / 100).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const aplicarMascaraMoeda = (valor: string) => {
    const digitos = valor.replace(/\D/g, '').slice(0, 10);
    return digitos ? formatarValorInput(Number(digitos)) : '';
};
const dataBR = (value: string) => value ? new Date(`${value}T00:00:00`).toLocaleDateString('pt-BR') : '—';
const podeEditar = (c: Contrato) => c.status !== 'ENCERRADO' && c.status !== 'RESCINDIDO';
const podeExcluir = (c: Contrato) => podeEditar(c);
const statusLabel: Record<StatusContrato, string> = {
    RASCUNHO: 'Rascunho', ATIVO: 'Ativo', ENCERRADO: 'Encerrado', RESCINDIDO: 'Rescindido',
};

function mensagemErro(error: unknown) { return getApiError(error, 'Ocorreu um erro ao processar o contrato.'); }

export function Contratos() {
    const [parametros, setParametros] = useSearchParams();
    const novoPeloDashboard = parametros.get('novo') === '1';
    const [contratos, setContratos] = useState<Contrato[]>([]);
    const [busca, setBusca] = useState('');
    const [status, setStatus] = useState('');
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');
    const [modalAberto, setModalAberto] = useState(false);
    const [salvando, setSalvando] = useState(false);
    const [contratoEdicao, setContratoEdicao] = useState<Contrato | null>(null);
    const [erroFormulario, setErroFormulario] = useState('');

    const carregar = useCallback(async (termo: string, statusAtual: string) => {
        setCarregando(true);
        setErro('');
        try {
            const filtros: { clienteNome?: string; projetoId?: number; status?: StatusContrato } = {};
            const texto = termo.trim();
            if (/^\d+$/.test(texto)) filtros.projetoId = Number(texto);
            else if (texto) filtros.clienteNome = texto;
            if (statusAtual) filtros.status = statusAtual as StatusContrato;
            setContratos(await contratoService.listar(filtros));
        } catch (error) {
            setErro(mensagemErro(error));
        } finally {
            setCarregando(false);
        }
    }, []);

    // eslint-disable-next-line react-hooks/set-state-in-effect
    useEffect(() => { void carregar('', ''); }, [carregar]);

    function abrirNovo() {
        setContratoEdicao(null);
        setErroFormulario('');
        setModalAberto(true);
    }

    function abrirEdicao(contrato: Contrato) {
        setContratoEdicao(contrato);
        setErroFormulario('');
        setModalAberto(true);
    }

    function fecharFormulario() {
        setModalAberto(false);
        if (novoPeloDashboard) {
            const proximos = new URLSearchParams(parametros);
            proximos.delete('novo');
            setParametros(proximos, { replace: true });
        }
    }

    async function salvar(payload: ContratoPayload) {
        if (salvando) return;
        setSalvando(true);
        setErroFormulario('');
        try {
            if (contratoEdicao) await contratoService.atualizar(contratoEdicao.id, payload);
            else await contratoService.criar(payload);
            fecharFormulario();
            await carregar(busca, status);
        } catch (error) {
            setErroFormulario(mensagemErro(error));
        } finally {
            setSalvando(false);
        }
    }

    async function excluir(contrato: Contrato) {
        const confirmado = window.confirm(`Deseja realmente excluir o contrato de ${contrato.clienteNome}? Esta operação não pode ser desfeita.`);
        if (!confirmado) return;
        try {
            await contratoService.excluir(contrato.id);
            await carregar(busca, status);
        } catch (error) {
            setErro(mensagemErro(error));
        }
    }

    return (
        <div className="contracts-page">
            <header className="contracts-header app-page-header">
                <div>
                    <h1>Contratos</h1>
                    <p className="page-header-breadcrumb">Hut8&nbsp; / &nbsp;Financeiro&nbsp; / &nbsp;Contratos</p>
                </div>
                <button className="contracts-new-button page-header-action" type="button" onClick={abrirNovo}>+ Novo contrato</button>
            </header>

            <section className="contracts-content" aria-label="Consulta de contratos">
                <label htmlFor="contracts-search">Buscar contrato, projeto ou cliente</label>
                <form className="contracts-search-row" onSubmit={(event) => { event.preventDefault(); void carregar(busca, status); }}>
                    <input id="contracts-search" type="search" maxLength={35} placeholder="⌕  Pesquisar por cliente ou ID do projeto..." value={busca} onChange={(event) => setBusca(event.target.value)} />
                    <select aria-label="Filtrar por status" value={status} onChange={(event) => { setStatus(event.target.value); void carregar(busca, event.target.value); }}>
                        <option value="">Todos os status</option>
                        <option value="RASCUNHO">Rascunho</option>
                        <option value="ATIVO">Ativo</option>
                        <option value="ENCERRADO">Encerrado</option>
                        <option value="RESCINDIDO">Rescindido</option>
                    </select>
                    <button type="submit" className="contracts-filter-button">Pesquisar</button>
                </form>

                {erro && <div className="contracts-feedback contracts-feedback-error" role="alert">{erro}</div>}
                {carregando ? <p className="contracts-feedback">Carregando contratos...</p> :
                    contratos.length === 0 ? <div className="contracts-empty">{erro ? 'Não foi possível carregar os contratos.' : 'Nenhum contrato encontrado.'}</div> :
                        <div className="contracts-grid" aria-live="polite">
                            {contratos.map((contrato) => (
                                <article className="contract-card" key={contrato.id}>
                                    <div className="contract-card-heading">
                                        <h2>Contrato #{contrato.id} • Projeto {contrato.projetoId}</h2>
                                        <span className={`contract-status status-${contrato.status.toLowerCase()}`}>{statusLabel[contrato.status]}</span>
                                    </div>
                                    <p className="contract-client">Cliente: {contrato.clienteNome}</p>
                                    <p className="contract-total-label">Valor total do contrato</p>
                                    <strong className="contract-total">{moeda.format(contrato.valorTotal)}</strong>
                                    <p className="contract-dates">{dataBR(contrato.dataInicio)} – {dataBR(contrato.dataFim)}</p>
                                    <div className="contract-card-actions">
                                        <Link to={`/app/contratos/${contrato.id}`}>Ver detalhes e prestações →</Link>
                                        {podeEditar(contrato) && <button type="button" onClick={() => abrirEdicao(contrato)}>Editar</button>}
                                        {podeExcluir(contrato) && <button type="button" className="contract-delete" onClick={() => void excluir(contrato)}>Excluir</button>}
                                    </div>
                                </article>
                            ))}
                        </div>}
            </section>

            {(modalAberto || novoPeloDashboard) && <ContratoForm contrato={contratoEdicao} salvando={salvando} erro={erroFormulario} onClose={() => !salvando && fecharFormulario()} onSave={salvar} />}
        </div>
    );
}

interface FormProps {
    contrato: Contrato | null;
    salvando: boolean;
    erro: string;
    onClose: () => void;
    onSave: (payload: ContratoPayload) => void;
}

function ContratoForm({ contrato, salvando, erro, onClose, onSave }: FormProps) {
    const [projetoId, setProjetoId] = useState(String(contrato?.projetoId ?? ''));
    const [clienteNome, setClienteNome] = useState(contrato?.clienteNome ?? '');
    const [arquivoRef, setArquivoRef] = useState(contrato?.arquivoRef ?? '');
    const [valorTotal, setValorTotal] = useState(contrato ? contrato.valorTotal.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '');
    const [dataInicio, setDataInicio] = useState(contrato?.dataInicio ?? '');
    const [dataFim, setDataFim] = useState(contrato?.dataFim ?? '');
    const [validacao, setValidacao] = useState('');

    function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const projeto = Number(projetoId);
        const valor = Number(valorTotal.trim().replace(/\./g, '').replace(',', '.'));
        if (!Number.isInteger(projeto) || projeto <= 0) return setValidacao('Informe um ID de projeto positivo.');
        if (!clienteNome.trim() || clienteNome.trim().length > 35) return setValidacao('Informe o nome do cliente (máximo de 35 caracteres).');
        if (!Number.isFinite(valor) || valor <= 0 || valor > 9999999.99 || !/^\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?$|^\d+(?:,\d{1,2})?$/.test(valorTotal.trim())) return setValidacao('Informe um valor válido, maior que zero e com até duas casas decimais.');
        if (!dataInicio || !dataFim) return setValidacao('Informe as datas de início e fim.');
        if (dataFim < dataInicio) return setValidacao('A data de fim não pode ser anterior à data de início.');
        setValidacao('');
        onSave({ projetoId: projeto, clienteNome: clienteNome.trim(), arquivoRef: arquivoRef.trim() || null, valorTotal: valor, dataInicio, dataFim });
    }

    return <div className="contract-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
        <section className="contract-modal" role="dialog" aria-modal="true" aria-labelledby="contract-form-title">
            <header><h2 id="contract-form-title">{contrato ? 'Editar contrato' : 'Novo contrato'}</h2><button type="button" onClick={onClose} aria-label="Fechar">×</button></header>
            <form onSubmit={submit}>
                <label>ID do projeto<input type="number" min="1" step="1" required value={projetoId} onChange={(e) => setProjetoId(e.target.value)} /></label>
                <label>Nome do cliente<input type="text" maxLength={35} required value={clienteNome} onChange={(e) => setClienteNome(e.target.value)} /></label>
                <label>Referência do arquivo (opcional)<input type="text" value={arquivoRef} onChange={(e) => setArquivoRef(e.target.value)} /></label>
                <label>Valor total (R$)<input inputMode="decimal" placeholder="0,00" required value={valorTotal} onChange={(e) => setValorTotal(aplicarMascaraMoeda(e.target.value))} /></label>
                <div className="contract-form-dates">
                    <label>Data de início<input type="date" required value={dataInicio} onChange={(e) => setDataInicio(e.target.value)} /></label>
                    <label>Data de fim<input type="date" min={dataInicio || undefined} required value={dataFim} onChange={(e) => setDataFim(e.target.value)} /></label>
                </div>
                {(validacao || erro) && <p className="contracts-feedback contracts-feedback-error" role="alert">{validacao || erro}</p>}
                <footer><button type="button" className="contract-cancel-button" onClick={onClose} disabled={salvando}>Cancelar</button><button type="submit" className="contracts-new-button" disabled={salvando}>{salvando ? 'Salvando...' : contrato ? 'Salvar alterações' : 'Cadastrar contrato'}</button></footer>
            </form>
        </section>
    </div>;
}

export function ContratoDetalhes() {
    const { id } = useParams();
    const navigate = useNavigate();
    const contratoId = Number(id);
    const [contrato, setContrato] = useState<Contrato | null>(null);
    const [prestacoes, setPrestacoes] = useState<PrestacaoContrato[]>([]);
    const [carregando, setCarregando] = useState(true);
    const [carregandoPrestacoes, setCarregandoPrestacoes] = useState(true);
    const [erro, setErro] = useState('');
    const [erroPrestacoes, setErroPrestacoes] = useState('');
    const [operando, setOperando] = useState(false);
    const [modalEdicao, setModalEdicao] = useState(false);
    const [erroEdicao, setErroEdicao] = useState('');
    const [modalPrestacao, setModalPrestacao] = useState(false);
    const [prestacaoEdicao, setPrestacaoEdicao] = useState<PrestacaoContrato | null>(null);
    const [erroPrestacao, setErroPrestacao] = useState('');
    const [prestacaoOperando, setPrestacaoOperando] = useState(false);
    const [prestacaoExpandida, setPrestacaoExpandida] = useState<number | null>(null);
    const [comprovantes, setComprovantes] = useState<Record<number, Comprovante[]>>({});
    const [carregandoComprovantes, setCarregandoComprovantes] = useState<number | null>(null);
    const [erroComprovantes, setErroComprovantes] = useState<Record<number, string>>({});
    const [formComprovante, setFormComprovante] = useState<number | null>(null);
    const [comprovanteTipo, setComprovanteTipo] = useState<ComprovanteTipo>('RECIBO');
    const [comprovanteArquivo, setComprovanteArquivo] = useState('');
    const [comprovanteNumero, setComprovanteNumero] = useState('');
    const [comprovanteValor, setComprovanteValor] = useState('');
    const [comprovanteData, setComprovanteData] = useState('');
    const [salvandoComprovante, setSalvandoComprovante] = useState(false);

    const carregarPrestacoes = useCallback(async () => {
        setCarregandoPrestacoes(true);
        setErroPrestacoes('');
        try { setPrestacoes(await contratoService.listarPrestacoes(contratoId)); }
        catch (error) { setErroPrestacoes(mensagemErro(error)); }
        finally { setCarregandoPrestacoes(false); }
    }, [contratoId]);

    const carregar = useCallback(async () => {
        if (!Number.isInteger(contratoId) || contratoId <= 0) {
            setErro('Identificador de contrato inválido.'); setCarregando(false); setCarregandoPrestacoes(false); return;
        }
        setCarregando(true); setErro('');
        try { setContrato(await contratoService.buscarPorId(contratoId)); }
        catch (error) { setErro(mensagemErro(error)); }
        finally { setCarregando(false); }
        await carregarPrestacoes();
    }, [contratoId, carregarPrestacoes]);
    // A consulta por ID sincroniza o estado da tela com os recursos remotos.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    useEffect(() => { void carregar(); }, [carregar]);

    const resumo = useMemo(() => {
        const ativas = prestacoes.filter((p) => p.status !== 'CANCELADA');
        return {
            parcelado: ativas.reduce((soma, p) => soma + Number(p.valor), 0),
            recebido: ativas.filter((p) => p.status === 'PAGA').reduce((soma, p) => soma + Number(p.valor), 0),
            pendente: ativas.filter((p) => p.status === 'PENDENTE').reduce((soma, p) => soma + Number(p.valor), 0),
        };
    }, [prestacoes]);

    async function alterarStatus(novoStatus: StatusContrato) {
        if (!contrato || operando) return;
        if (!window.confirm(`Confirma a alteração do status para ${statusLabel[novoStatus]}?`)) return;
        setOperando(true); setErro('');
        try { setContrato(await contratoService.atualizarStatus(contrato.id, novoStatus)); }
        catch (error) { setErro(mensagemErro(error)); }
        finally { setOperando(false); }
    }

    async function excluir() {
        if (!contrato || operando || !window.confirm(`Excluir definitivamente o contrato #${contrato.id}?`)) return;
        setOperando(true);
        try { await contratoService.excluir(contrato.id); navigate('/app/contratos', { replace: true }); }
        catch (error) { setErro(mensagemErro(error)); setOperando(false); }
    }

    async function salvarEdicao(payload: ContratoPayload) {
        if (!contrato || operando) return;
        setOperando(true); setErroEdicao('');
        try { setContrato(await contratoService.atualizar(contrato.id, payload)); setModalEdicao(false); }
        catch (error) { setErroEdicao(mensagemErro(error)); }
        finally { setOperando(false); }
    }

    function abrirPrestacao(prestacao: PrestacaoContrato | null = null) {
        setPrestacaoEdicao(prestacao); setErroPrestacao(''); setModalPrestacao(true);
    }

    async function salvarPrestacao(payload: PrestacaoPayload) {
        if (!contrato || prestacaoOperando) return;
        setPrestacaoOperando(true); setErroPrestacao('');
        try {
            if (prestacaoEdicao) await contratoService.atualizarPrestacao(contrato.id, prestacaoEdicao.id, payload);
            else await contratoService.criarPrestacao(contrato.id, payload);
            setModalPrestacao(false); setPrestacaoEdicao(null); await carregarPrestacoes();
        } catch (error) { setErroPrestacao(mensagemErro(error)); }
        finally { setPrestacaoOperando(false); }
    }

    async function cancelarPrestacao(prestacao: PrestacaoContrato) {
        if (!contrato || prestacaoOperando || !window.confirm(`Cancelar a prestação ${prestacao.numero}?`)) return;
        setPrestacaoOperando(true); setErroPrestacoes('');
        try { await contratoService.cancelarPrestacao(contrato.id, prestacao.id); await carregarPrestacoes(); }
        catch (error) { setErroPrestacoes(mensagemErro(error)); }
        finally { setPrestacaoOperando(false); }
    }

    async function alternarComprovantes(prestacaoId: number) {
        if (prestacaoExpandida === prestacaoId) { setPrestacaoExpandida(null); setFormComprovante(null); return; }
        setPrestacaoExpandida(prestacaoId); setFormComprovante(null);
        setCarregandoComprovantes(prestacaoId);
        try {
            const lista = await transacaoService.listarComprovantesPrestacao(prestacaoId);
            setComprovantes((atual) => ({ ...atual, [prestacaoId]: lista }));
            setErroComprovantes((atual) => ({ ...atual, [prestacaoId]: '' }));
        } catch (error) {
            setErroComprovantes((atual) => ({ ...atual, [prestacaoId]: mensagemErro(error) }));
        } finally { setCarregandoComprovantes(null); }
    }

    async function salvarComprovante(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (formComprovante === null || salvandoComprovante) return;
        const valor = comprovanteValor.trim() ? Number(comprovanteValor.replace(/\./g, '').replace(',', '.')) : null;
        if (valor !== null && (!Number.isFinite(valor) || valor <= 0)) {
            setErroComprovantes((atual) => ({ ...atual, [formComprovante]: 'Informe um valor válido para o documento.' })); return;
        }
        const payload: ComprovanteCreate = {
            tipo: comprovanteTipo, arquivoRef: comprovanteArquivo.trim(),
            numeroNotaFiscal: comprovanteNumero.trim() || null, valor, dataDocumento: comprovanteData || null,
        };
        setSalvandoComprovante(true);
        try {
            await transacaoService.adicionarComprovantePrestacao(formComprovante, payload);
            const lista = await transacaoService.listarComprovantesPrestacao(formComprovante);
            setComprovantes((atual) => ({ ...atual, [formComprovante]: lista }));
            setErroComprovantes((atual) => ({ ...atual, [formComprovante]: '' }));
            setFormComprovante(null); setComprovanteArquivo(''); setComprovanteNumero(''); setComprovanteValor(''); setComprovanteData('');
            await carregarPrestacoes();
        } catch (error) {
            setErroComprovantes((atual) => ({ ...atual, [formComprovante]: mensagemErro(error) }));
        } finally { setSalvandoComprovante(false); }
    }

    return <div className="contracts-page">
        <header className="contracts-header app-page-header">
            <div>
                <h1>Detalhes do contrato</h1>
                <p className="page-header-breadcrumb">Hut8&nbsp; / &nbsp;Financeiro&nbsp; / &nbsp;Contratos&nbsp; / &nbsp;Detalhes</p>
            </div>
            {contrato && podeEditar(contrato) && <button className="contracts-new-button page-header-action" type="button" onClick={() => setModalEdicao(true)}>Editar contrato</button>}
            <p className="contracts-disclaimer page-header-disclaimer">GESTÃO FINANCEIRA <span>•</span> DADOS REAIS</p>
        </header>
        <section className="contracts-content contract-details-content">
            <Link className="contract-back-link" to="/app/contratos">← Voltar para contratos</Link>
            {carregando ? <p className="contracts-feedback">Carregando contrato...</p> : erro && !contrato ? <div className="contracts-feedback contracts-feedback-error" role="alert">{erro}</div> : contrato && <>
                {erro && <div className="contracts-feedback contracts-feedback-error" role="alert">{erro}</div>}
                <article className="contract-detail-card">
                    <div className="contract-card-heading"><h2>Projeto {contrato.projetoId} • {contrato.clienteNome}</h2><span className={`contract-status status-${contrato.status.toLowerCase()}`}>{statusLabel[contrato.status]}</span></div>
                    <div className="contract-detail-values">
                        <div><span>Valor total</span><strong>{moeda.format(contrato.valorTotal)}</strong></div>
                        <div><span>Início</span><strong>{dataBR(contrato.dataInicio)}</strong></div>
                        <div><span>Fim</span><strong>{dataBR(contrato.dataFim)}</strong></div>
                    </div>
                    {contrato.arquivoRef && <p className="contract-file-reference">Referência: {contrato.arquivoRef}</p>}
                    <div className="contract-status-actions">
                        {contrato.status === 'RASCUNHO' && <button type="button" disabled={operando} onClick={() => void alterarStatus('ATIVO')}>Ativar contrato</button>}
                        {contrato.status === 'ATIVO' && <><button type="button" disabled={operando} onClick={() => void alterarStatus('ENCERRADO')}>Encerrar contrato</button><button type="button" disabled={operando} onClick={() => void alterarStatus('RESCINDIDO')}>Rescindir contrato</button></>}
                        {podeExcluir(contrato) && <button type="button" className="contract-delete" disabled={operando} onClick={() => void excluir()}>Excluir contrato</button>}
                    </div>
                </article>
                <section className="contract-detail-card installments-card">
                    <div className="installments-heading">
                        <div><h2>Prestações do contrato</h2><p>Parcelas, vencimentos, recebimentos e comprovantes vinculados.</p></div>
                        {contrato.status === 'ATIVO' && <button className="contracts-new-button installments-add-button" type="button" onClick={() => abrirPrestacao()}>+ Nova prestação</button>}
                    </div>
                    {erroPrestacoes && <div className="contracts-feedback contracts-feedback-error" role="alert">{erroPrestacoes}</div>}
                    {carregandoPrestacoes ? <p className="contracts-feedback">Carregando prestações...</p> : <>
                        <div className="installments-summary">
                            <div><span>Valor contratado</span><strong>{moeda.format(contrato.valorTotal)}</strong></div>
                            <div><span>Valor parcelado</span><strong>{moeda.format(resumo.parcelado)}</strong></div>
                            <div><span>Recebido</span><strong>{moeda.format(resumo.recebido)}</strong></div>
                            <div><span>Pendente</span><strong>{moeda.format(resumo.pendente)}</strong></div>
                        </div>
                        {prestacoes.length === 0 ? <div className="contracts-empty">Este contrato ainda não possui prestações cadastradas.</div> : <div className="installments-table-wrap">
                            <table className="installments-table">
                                <thead><tr><th>Parcela</th><th>Vencimento</th><th>Valor</th><th>Situação</th><th>Comprovantes</th><th>Ações</th></tr></thead>
                                <tbody>{prestacoes.map((prestacao) => <tr key={prestacao.id}>
                                    <td>{prestacao.numero}ª prestação</td><td>{dataBR(prestacao.dataVencimento)}</td><td>{moeda.format(Number(prestacao.valor))}</td>
                                    <td><span className={`installment-status installment-${prestacao.status.toLowerCase()}`}>{prestacao.status === 'PAGA' ? 'Paga' : prestacao.status === 'PENDENTE' ? 'Pendente' : 'Cancelada'}</span></td>
                                    <td>{prestacao.quantidadeComprovantes}</td>
                                    <td><div className="installment-actions">
                                        <button type="button" onClick={() => void alternarComprovantes(prestacao.id)}>Comprovantes</button>
                                        {prestacao.status === 'PENDENTE' && contrato.status === 'ATIVO' && prestacao.transacaoId === null && prestacao.quantidadeComprovantes === 0 && <button type="button" disabled={prestacaoOperando} onClick={() => abrirPrestacao(prestacao)}>Editar</button>}
                                        {prestacao.status === 'PENDENTE' && contrato.status === 'ATIVO' && prestacao.transacaoId === null && prestacao.quantidadeComprovantes === 0 && <button type="button" disabled={prestacaoOperando} onClick={() => void cancelarPrestacao(prestacao)}>Cancelar</button>}
                                        {prestacao.status === 'PENDENTE' && <Link to={`/app/transacoes/nova?prestacaoId=${prestacao.id}`}>Registrar recebimento</Link>}
                                    </div></td>
                                </tr>)}</tbody>
                            </table>
                        </div>}
                        {prestacaoExpandida !== null && <section className="installment-documents">
                            <h3>Comprovantes da prestação {prestacoes.find((p) => p.id === prestacaoExpandida)?.numero ?? ''}</h3>
                            {carregandoComprovantes === prestacaoExpandida ? <p>Carregando comprovantes...</p> : <>
                                {erroComprovantes[prestacaoExpandida] && <p className="contracts-feedback contracts-feedback-error">{erroComprovantes[prestacaoExpandida]}</p>}
                                {(comprovantes[prestacaoExpandida] ?? []).length === 0 ? <p>Nenhum comprovante cadastrado.</p> : <ul>{comprovantes[prestacaoExpandida].map((doc) => <li key={doc.id}>
                                    <span>{doc.tipo.replaceAll('_', ' ')}{doc.numeroNotaFiscal ? ` • ${doc.numeroNotaFiscal}` : ''}{doc.valor ? ` • ${moeda.format(Number(doc.valor))}` : ''}</span>
                                    {/^(https?:\/\/)/i.test(doc.arquivoRef) ? <a href={doc.arquivoRef} target="_blank" rel="noreferrer">Abrir documento</a> : <span>{doc.arquivoRef}</span>}
                                </li>)}</ul>}
                                {formComprovante === prestacaoExpandida ? <form className="installment-proof-form" onSubmit={salvarComprovante}>
                                    <h4>Adicionar comprovante</h4>
                                    <div className="installment-proof-fields">
                                        <label>Tipo<select value={comprovanteTipo} onChange={(e) => setComprovanteTipo(e.target.value as ComprovanteTipo)}><option value="RECIBO">Recibo</option><option value="NOTA_FISCAL">Nota fiscal</option><option value="COMPROVANTE_PAGAMENTO">Comprovante de pagamento</option><option value="OUTRO">Outro</option></select></label>
                                        <label>Número do documento<input maxLength={60} value={comprovanteNumero} onChange={(e) => setComprovanteNumero(e.target.value)} /></label>
                                        <label>Valor (opcional)<input inputMode="decimal" placeholder="0,00" value={comprovanteValor} onChange={(e) => setComprovanteValor(e.target.value)} /></label>
                                        <label>Data do documento<input type="date" value={comprovanteData} onChange={(e) => setComprovanteData(e.target.value)} /></label>
                                    </div>
                                    <label>Referência/URL do arquivo<input required maxLength={4000} value={comprovanteArquivo} onChange={(e) => setComprovanteArquivo(e.target.value)} placeholder="https://... ou caminho do arquivo" /></label>
                                    <div className="installment-actions"><button type="submit" disabled={salvandoComprovante || !comprovanteArquivo.trim()}>{salvandoComprovante ? 'Salvando...' : 'Salvar comprovante'}</button><button type="button" disabled={salvandoComprovante} onClick={() => setFormComprovante(null)}>Cancelar</button></div>
                                </form> : <button type="button" className="installment-add-proof" onClick={() => { setFormComprovante(prestacaoExpandida); setErroComprovantes((atual) => ({ ...atual, [prestacaoExpandida]: '' })); }}>+ Adicionar comprovante</button>}
                            </>}
                        </section>}
                    </>}
                </section>
            </>}
        </section>
        {modalEdicao && contrato && <ContratoForm contrato={contrato} salvando={operando} erro={erroEdicao} onClose={() => !operando && setModalEdicao(false)} onSave={salvarEdicao} />}
        {modalPrestacao && <PrestacaoForm prestacao={prestacaoEdicao} salvando={prestacaoOperando} erro={erroPrestacao} onClose={() => !prestacaoOperando && setModalPrestacao(false)} onSave={salvarPrestacao} />}
    </div>;
}

interface PrestacaoFormProps {
    prestacao: PrestacaoContrato | null;
    salvando: boolean;
    erro: string;
    onClose: () => void;
    onSave: (payload: PrestacaoPayload) => void;
}

function PrestacaoForm({ prestacao, salvando, erro, onClose, onSave }: PrestacaoFormProps) {
    const [numero, setNumero] = useState(String(prestacao?.numero ?? ''));
    const [valor, setValor] = useState(prestacao ? Number(prestacao.valor).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '');
    const [dataVencimento, setDataVencimento] = useState(prestacao?.dataVencimento ?? '');
    const [validacao, setValidacao] = useState('');

    function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const numeroInformado = Number(numero);
        const valorInformado = Number(valor.trim().replace(/\./g, '').replace(',', '.'));
        if (!Number.isInteger(numeroInformado) || numeroInformado <= 0) return setValidacao('Informe um número de prestação positivo.');
        if (!Number.isFinite(valorInformado) || valorInformado <= 0 || valorInformado > 9999999.99) return setValidacao('Informe um valor positivo válido.');
        if (!/^\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?$|^\d+(?:,\d{1,2})?$/.test(valor.trim())) return setValidacao('Informe um valor com até duas casas decimais.');
        if (!dataVencimento) return setValidacao('Informe a data de vencimento.');
        setValidacao(''); onSave({ numero: numeroInformado, valor: valorInformado, dataVencimento });
    }

    return <div className="contract-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
        <section className="contract-modal" role="dialog" aria-modal="true" aria-labelledby="installment-form-title">
            <header><h2 id="installment-form-title">{prestacao ? 'Editar prestação' : 'Nova prestação'}</h2><button type="button" onClick={onClose} aria-label="Fechar">×</button></header>
            <form onSubmit={submit}>
                <label>Número da prestação<input type="number" min="1" step="1" required value={numero} onChange={(e) => setNumero(e.target.value)} /></label>
                <label>Valor (R$)<input inputMode="decimal" placeholder="0,00" required value={valor} onChange={(e) => setValor(aplicarMascaraMoeda(e.target.value))} /></label>
                <label>Data de vencimento<input type="date" required value={dataVencimento} onChange={(e) => setDataVencimento(e.target.value)} /></label>
                {(validacao || erro) && <p className="contracts-feedback contracts-feedback-error" role="alert">{validacao || erro}</p>}
                <footer><button type="button" className="contract-cancel-button" onClick={onClose} disabled={salvando}>Cancelar</button><button type="submit" className="contracts-new-button" disabled={salvando}>{salvando ? 'Salvando...' : 'Salvar prestação'}</button></footer>
            </form>
        </section>
    </div>;
}
