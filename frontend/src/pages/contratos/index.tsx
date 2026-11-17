import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { contratoService } from '../../services/contratoService';
import { getApiError } from '../../services/api';
import type { Contrato, ContratoPayload, StatusContrato } from '../../types/contrato';
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

    // A busca inicial sincroniza o estado da tela com a API.
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

    async function salvar(payload: ContratoPayload) {
        if (salvando) return;
        setSalvando(true);
        setErroFormulario('');
        try {
            if (contratoEdicao) await contratoService.atualizar(contratoEdicao.id, payload);
            else await contratoService.criar(payload);
            setModalAberto(false);
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
            <header className="contracts-header">
                <div>
                    <h1>Contratos</h1>
                    <p className="contracts-breadcrumb">Hut8&nbsp; / &nbsp;Financeiro&nbsp; / &nbsp;Contratos</p>
                </div>
                <button className="contracts-new-button" type="button" onClick={abrirNovo}>+ Novo contrato</button>
                <p className="contracts-disclaimer">GESTÃO FINANCEIRA <span>•</span> DADOS REAIS</p>
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

            {modalAberto && <ContratoForm contrato={contratoEdicao} salvando={salvando} erro={erroFormulario} onClose={() => !salvando && setModalAberto(false)} onSave={salvar} />}
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
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');
    const [operando, setOperando] = useState(false);
    const [modalEdicao, setModalEdicao] = useState(false);
    const [erroEdicao, setErroEdicao] = useState('');

    const carregar = useCallback(async () => {
        if (!Number.isInteger(contratoId) || contratoId <= 0) { setErro('Identificador de contrato inválido.'); setCarregando(false); return; }
        setCarregando(true); setErro('');
        try { setContrato(await contratoService.buscarPorId(contratoId)); }
        catch (error) { setErro(mensagemErro(error)); }
        finally { setCarregando(false); }
    }, [contratoId]);
    // A consulta por ID sincroniza o detalhe com o recurso remoto.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    useEffect(() => { void carregar(); }, [carregar]);

    async function alterarStatus(novoStatus: StatusContrato) {
        if (!contrato || operando) return;
        const rotulo = statusLabel[novoStatus];
        if (!window.confirm(`Confirma a alteração do status para ${rotulo}?`)) return;
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

    return <div className="contracts-page">
        <header className="contracts-header">
            <div><h1>Detalhes do contrato</h1><p className="contracts-breadcrumb">Hut8&nbsp; / &nbsp;Financeiro&nbsp; / &nbsp;Contratos&nbsp; / &nbsp;Detalhes</p></div>
            {contrato && podeEditar(contrato) && <button className="contracts-new-button" type="button" onClick={() => setModalEdicao(true)}>Editar contrato</button>}
            <p className="contracts-disclaimer">GESTÃO FINANCEIRA <span>•</span> DADOS REAIS</p>
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
                    <h2>Prestações do contrato</h2>
                    <div className="contracts-empty">A consulta e o gerenciamento de prestações ainda não possuem endpoints disponíveis nesta API. Os dados não serão simulados.</div>
                </section>
            </>}
        </section>
        {modalEdicao && contrato && <ContratoForm contrato={contrato} salvando={operando} erro={erroEdicao} onClose={() => !operando && setModalEdicao(false)} onSave={salvarEdicao} />}
    </div>;
}
