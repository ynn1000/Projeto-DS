import { useEffect, useState } from 'react';
import { Link, useLocation, useOutletContext } from 'react-router-dom';
import { getApiError } from '../../services/api';
import { orcamentoService } from '../../services/orcamentoService';
import type { Orcamento, StatusOrcamento } from '../../types/orcamento';
import type { RoleUsuario } from '../../types/usuario';
import { data, moeda, rotulosStatus } from './formatos';
import './solicitar_orcamento.css';

const filtros: { valor?: StatusOrcamento; rotulo: string }[] = [
    { rotulo: 'Todas' },
    { valor: 'PENDENTE', rotulo: 'Pendentes' },
    { valor: 'EM_ANALISE', rotulo: 'Em análise' },
    { valor: 'APROVADO', rotulo: 'Aprovadas' },
    { valor: 'REJEITADO', rotulo: 'Rejeitadas' },
];

export function Orcamentos() {
    const admin = useOutletContext<RoleUsuario>() === 'ADMIN';
    const { state } = useLocation();
    const retorno = state as { pedidoCriado?: number; decisaoRegistrada?: boolean } | null;
    const [pedidos, setPedidos] = useState<Orcamento[]>([]);
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');
    const [tentativa, setTentativa] = useState(0);
    const [filtro, setFiltro] = useState<StatusOrcamento>();

    useEffect(() => {
        let ativo = true;
        const consulta = admin ? orcamentoService.listarTodas(filtro) : orcamentoService.listarMinhas();
        consulta
            .then((lista) => { if (ativo) setPedidos(lista); })
            .catch((error: unknown) => {
                if (ativo) setErro(getApiError(error, 'Não foi possível carregar as solicitações.'));
            })
            .finally(() => { if (ativo) setCarregando(false); });
        return () => { ativo = false; };
    }, [admin, filtro, tentativa]);

    return (
        <div className="budget-request-page">
            <header className="app-page-header">
                <div>
                    <h1>{admin ? 'Orçamentos' : 'Minhas solicitações'}</h1>
                    <p className="page-header-breadcrumb">
                        {admin ? 'Hut8 / Diretor / Solicitações de orçamento' : 'Hut8 / Colaborador / Acompanhar solicitações'}
                    </p>
                </div>
                <Link className="page-header-action budget-new-link" to="/app/orcamentos/solicitar">+ Nova solicitação</Link>
            </header>

            <div className="budget-request-content budget-history">
                {retorno?.pedidoCriado && (
                    <p className="budget-feedback budget-success" role="status">
                        Solicitação nº {retorno.pedidoCriado} enviada com sucesso.
                    </p>
                )}
                {retorno?.decisaoRegistrada && (
                    <p className="budget-feedback budget-success" role="status">Decisão registrada com sucesso.</p>
                )}
                <section className="budget-request-card" aria-labelledby="budget-history-title">
                    <h2 id="budget-history-title">{admin ? 'Avaliar solicitações da equipe' : 'Meus pedidos enviados'}</h2>
                    {admin && (
                        <div className="budget-filters" role="group" aria-label="Filtrar por status">
                            {filtros.map(({ valor, rotulo }) => (
                                <button key={rotulo} type="button" aria-pressed={filtro === valor} onClick={() => {
                                    if (filtro === valor) return;
                                    setErro('');
                                    setCarregando(true);
                                    setFiltro(valor);
                                }}>{rotulo}</button>
                            ))}
                        </div>
                    )}
                    {carregando && <p role="status">Carregando solicitações…</p>}
                    {erro && (
                        <div className="budget-feedback budget-error">
                            <span role="alert">{erro}</span>{' '}
                            <button type="button" onClick={() => {
                                setErro('');
                                setCarregando(true);
                                setTentativa((atual) => atual + 1);
                            }}>Tentar novamente</button>
                        </div>
                    )}
                    {!carregando && !erro && (
                        pedidos.length ? (
                            <ul className="budget-history-list">
                                {pedidos.map((pedido) => (
                                    <li key={pedido.id}>
                                        <Link className="budget-history-item" to={String(pedido.id)}>
                                            <span className="budget-history-heading">
                                                <strong>{pedido.titulo}</strong>
                                                <span>{admin ? pedido.solicitanteNome : `Enviado em ${data.format(new Date(pedido.criadoEm))}`}</span>
                                                {!admin && <span className="budget-details-label">Ver detalhes da solicitação →</span>}
                                            </span>
                                            <span className="budget-history-meta">
                                                <strong>{moeda.format(pedido.valorSolicitado)}</strong>
                                                <span className="budget-status" data-status={pedido.status}>{rotulosStatus[pedido.status]}</span>
                                            </span>
                                        </Link>
                                    </li>
                                ))}
                            </ul>
                        ) : <p>{admin ? 'Nenhuma solicitação encontrada.' : 'Você ainda não enviou solicitações.'}</p>
                    )}
                </section>
            </div>
        </div>
    );
}
