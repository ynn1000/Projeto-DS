import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { getApiError } from '../../services/api';
import { orcamentoService } from '../../services/orcamentoService';
import type { Orcamento } from '../../types/orcamento';
import type { RoleUsuario } from '../../types/usuario';
import { data, moeda, rotulosStatus } from './formatos';
import './solicitar_orcamento.css';

function linkDoAnexo(referencia: string) {
    try {
        const url = new URL(referencia);
        return url.protocol === 'https:' || url.protocol === 'http:' ? url.href : undefined;
    } catch {
        return undefined;
    }
}

export function OrcamentoDetalhes() {
    const id = Number(useParams().id);
    const admin = useOutletContext<RoleUsuario>() === 'ADMIN';
    const navigate = useNavigate();
    const [pedido, setPedido] = useState<Orcamento>();
    const [erro, setErro] = useState('');
    const [erroDecisao, setErroDecisao] = useState('');
    const [tentativa, setTentativa] = useState(0);
    const [recusando, setRecusando] = useState(false);
    const [justificativa, setJustificativa] = useState('');
    const [salvando, setSalvando] = useState(false);

    useEffect(() => {
        if (!Number.isInteger(id) || id < 1 || id > 2147483647) return;
        let ativo = true;

        async function abrirSolicitacao() {
            const resultado = await orcamentoService.consultar(id);
            if (!ativo || !admin || resultado.status !== 'PENDENTE') return resultado;

            try {
                return await orcamentoService.alterarStatus(id, { status: 'EM_ANALISE' });
            } catch (error) {
                if (isAxiosError(error) && error.response?.status === 409) {
                    return orcamentoService.consultar(id);
                }
                throw error;
            }
        }

        abrirSolicitacao()
            .then((resultado) => { if (ativo) setPedido(resultado); })
            .catch((error: unknown) => {
                if (ativo) setErro(getApiError(error, 'Não foi possível abrir a solicitação.'));
            });
        return () => { ativo = false; };
    }, [admin, id, tentativa]);

    async function decidir(status: 'APROVADO' | 'REJEITADO') {
        if (salvando || !pedido) return;
        setErroDecisao('');
        if (status === 'REJEITADO' && !justificativa.trim()) {
            setErroDecisao('Informe a justificativa da rejeição.');
            return;
        }
        setSalvando(true);
        try {
            await orcamentoService.alterarStatus(pedido.id, {
                status,
                justificativa: status === 'REJEITADO' ? justificativa.trim() : undefined,
            });
            navigate('/app/orcamentos', { replace: true, state: { decisaoRegistrada: true } });
        } catch (error) {
            if (isAxiosError(error) && error.response?.status === 409) {
                setErroDecisao('O status mudou. Confira os dados atualizados antes de tentar novamente.');
                setPedido(undefined);
                setRecusando(false);
                setTentativa((atual) => atual + 1);
            } else {
                setErroDecisao(getApiError(error, 'Não foi possível registrar a decisão.'));
            }
        } finally {
            setSalvando(false);
        }
    }

    function rejeitar(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        void decidir('REJEITADO');
    }

    const invalido = !Number.isInteger(id) || id < 1 || id > 2147483647;
    const podeDecidir = admin && (pedido?.status === 'PENDENTE' || pedido?.status === 'EM_ANALISE');
    const anexo = pedido?.arquivoRef && linkDoAnexo(pedido.arquivoRef);

    return (
        <div className="budget-request-page">
            <header className="app-page-header">
                <div>
                    <h1>{admin ? 'Analisar orçamento' : 'Detalhes da solicitação'}</h1>
                    <p className="page-header-breadcrumb">Hut8 / {admin ? 'Diretor' : 'Colaborador'} / Detalhes da solicitação</p>
                </div>
            </header>
            <div className="budget-request-content budget-history">
                <Link className="budget-details-label" to="/app/orcamentos">← Voltar para solicitações</Link>
                {invalido && <p className="budget-feedback budget-error" role="alert">Solicitação inválida.</p>}
                {erro && (
                    <div className="budget-feedback budget-error">
                        <span role="alert">{erro}</span>{' '}
                        <button type="button" onClick={() => {
                            setErro('');
                            setTentativa((atual) => atual + 1);
                        }}>Tentar novamente</button>
                    </div>
                )}
                {erroDecisao && <p className="budget-feedback budget-error" role="alert">{erroDecisao}</p>}
                {!pedido && !erro && !invalido && <p role="status">Carregando solicitação…</p>}
                {pedido && (
                    <section className="budget-request-card budget-detail">
                        <div className="budget-detail-title">
                            <h2>{pedido.titulo}</h2>
                            <span className="budget-status" data-status={pedido.status}>{rotulosStatus[pedido.status]}</span>
                        </div>
                        <dl className="budget-detail-meta">
                            <div><dt>{admin ? 'Solicitado por' : 'Enviado em'}</dt><dd>{admin ? pedido.solicitanteNome : data.format(new Date(pedido.criadoEm))}</dd></div>
                            <div><dt>Valor estimado</dt><dd>{moeda.format(pedido.valorSolicitado)}</dd></div>
                        </dl>
                        <h3>Justificativa</h3>
                        <p className="budget-description">{pedido.descricao}</p>
                        <h3>Anexos enviados</h3>
                        <div className="budget-file">
                            {anexo ? <a className="budget-details-label" href={anexo} target="_blank" rel="noreferrer">{pedido.arquivoRef}</a> : pedido.arquivoRef || 'Nenhum anexo enviado.'}
                        </div>
                        {pedido.justificativaDecisao && (
                            <>
                                <h3>Justificativa da decisão</h3>
                                <p className="budget-description">{pedido.justificativaDecisao}</p>
                            </>
                        )}
                        {podeDecidir && (recusando ? (
                            <form onSubmit={rejeitar} className="budget-reject">
                                <label className="budget-field">Justificativa da rejeição
                                    <textarea required rows={3} value={justificativa} onChange={(event) => setJustificativa(event.target.value)} />
                                </label>
                                <div className="budget-form-actions">
                                    <button type="button" className="budget-cancel" disabled={salvando} onClick={() => {
                                        setRecusando(false);
                                        setErroDecisao('');
                                    }}>Cancelar</button>
                                    <button type="submit" className="budget-submit" disabled={salvando}>{salvando ? 'Registrando…' : 'Confirmar rejeição'}</button>
                                </div>
                            </form>
                        ) : (
                            <div className="budget-form-actions">
                                <button type="button" className="budget-cancel" disabled={salvando} onClick={() => {
                                    setErroDecisao('');
                                    setRecusando(true);
                                }}>Rejeitar solicitação</button>
                                <button type="button" className="budget-submit" disabled={salvando} onClick={() => void decidir('APROVADO')}>{salvando ? 'Registrando…' : 'Aprovar orçamento'}</button>
                            </div>
                        ))}
                    </section>
                )}
            </div>
        </div>
    );
}
