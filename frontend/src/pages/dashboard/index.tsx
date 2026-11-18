import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { getApiError } from '../../services/api';
import { dashboardService, type Caixa, type Lancamento, type Recebimentos, type TipoTransacao } from '../../services/dashboardService';
import { LancamentoDialog } from './LancamentoDialog';
import { moeda, resumirMeses } from './financeiro';
import './dashboard.css';

interface Painel {
    caixa: Caixa | null;
    transacoes: Lancamento[] | null;
    recebimentos: Recebimentos | null;
    erros: string[];
}

const dataCurta = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' });
const nomeMes = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' });

function Indicador({ titulo, valor, detalhe, carregando }: {
    titulo: string; valor?: number; detalhe: string; carregando: boolean;
}) {
    return <article className="dashboard-indicador">
        <h2>{titulo}</h2>
        <strong>{valor != null ? moeda.format(valor) : '—'}</strong>
        <p>{carregando ? 'Carregando…' : detalhe}</p>
    </article>;
}

function FluxoCaixa({ meses }: { meses: ReturnType<typeof resumirMeses> }) {
    const maior = Math.max(0, ...meses.map(mes => mes.liquido));
    const menor = Math.min(0, ...meses.map(mes => mes.liquido));
    const escala = maior - menor || 1;
    const altura = 114;
    const base = escala === 1 && maior === 0 && menor === 0 ? 122 : 8 + maior / escala * altura;
    return (
        <svg className="dashboard-chart" viewBox="0 0 480 150" role="img"
            aria-label={`Fluxo líquido dos últimos seis meses. ${meses.map(mes =>
                `${mes.rotulo} de ${mes.ano}: ${moeda.format(mes.liquido)}`).join('. ')}`}>
            {menor < 0 && <line className="dashboard-chart-baseline" x1="0" x2="480" y1={base} y2={base} />}
            {meses.map((mes, indice) => {
                const h = Math.max(2, Math.abs(mes.liquido) / escala * altura);
                const x = 40 + indice * 80;
                return <g key={mes.chave}>
                    <title>{mes.rotulo} {mes.ano} · {moeda.format(mes.liquido)}</title>
                    <rect x={x - 18} y={mes.liquido < 0 ? base : base - h} width="36" height={h} rx="4"
                        className={mes.liquido > 0 ? 'dashboard-bar-positive' : 'dashboard-bar-neutral'} />
                    <text x={x} y="143" textAnchor="middle">{mes.rotulo}</text>
                </g>;
            })}
        </svg>
    );
}

export function Dashboard() {
    const logado = Boolean(localStorage.getItem('session'));
    const [painel, setPainel] = useState<Painel>();
    const [versao, setVersao] = useState(0);
    const [tipoModal, setTipoModal] = useState<TipoTransacao | null>(null);
    const [salvo, setSalvo] = useState(false);

    useEffect(() => {
        if (!logado) return;
        const controller = new AbortController();
        const sinal = controller.signal;
        void Promise.allSettled([
            dashboardService.caixa(sinal),
            dashboardService.transacoes(sinal),
            dashboardService.recebimentos(sinal),
        ]).then(([caixa, transacoes, recebimentos]) => {
            if (sinal.aborted) return;
            const erros = [...new Set([caixa, transacoes, recebimentos].flatMap(resultado =>
                resultado.status === 'rejected' ? [getApiError(resultado.reason)] : []))];
            setPainel({
                caixa: caixa.status === 'fulfilled' ? caixa.value : null,
                transacoes: transacoes.status === 'fulfilled' ? transacoes.value : null,
                recebimentos: recebimentos.status === 'fulfilled' ? recebimentos.value : null,
                erros,
            });
        });
        return () => controller.abort();
    }, [logado, versao]);

    const carregando = logado && painel === undefined;
    const meses = useMemo(() => resumirMeses(painel?.transacoes ?? []), [painel?.transacoes]);
    const mes = painel?.transacoes ? meses.at(-1) : undefined;
    const recebimentos = painel?.recebimentos;
    const proximos = recebimentos?.proximosVencimentos;
    const indisponivel = logado ? 'Dados indisponíveis' : 'Entre para consultar';

    return (
        <div className="dashboard-page">
            <header className="dashboard-header app-page-header">
                <div>
                    <h1>Dashboard financeiro</h1>
                    <p className="page-header-breadcrumb">Hut8&nbsp; / &nbsp;Financeiro&nbsp; / &nbsp;Visão geral</p>
                </div>
                <button type="button" className="dashboard-primary page-header-action" disabled={!logado}
                    onClick={() => { setSalvo(false); setTipoModal('DESPESA'); }}>+ Novo lançamento</button>
                <p className="dashboard-periodo page-header-disclaimer">GESTÃO FINANCEIRA <span>•</span> {nomeMes.format(new Date())}</p>
            </header>

            <div className="dashboard-body">
                {!logado && <p className="dashboard-message"><Link to="/auth/login">Entre na sua conta</Link> para consultar o painel.</p>}
                {painel?.erros.length ? <div className="dashboard-message" role="alert">
                    <strong>Não foi possível carregar todos os dados.</strong>
                    {painel.erros.map(erro => <p key={erro}>{erro}</p>)}
                    <button type="button" className="dashboard-secondary"
                        onClick={() => { setPainel(undefined); setVersao(valor => valor + 1); }}>Tentar novamente</button>
                </div> : null}
                {salvo && <p className="dashboard-success" role="status">Lançamento salvo com sucesso.</p>}

                <section className="dashboard-indicadores" aria-label="Indicadores financeiros" aria-busy={carregando}>
                    <Indicador titulo="Saldo disponível" valor={painel?.caixa?.saldoAtual}
                        detalhe={painel?.caixa ? 'Saldo atual do Caixa' : indisponivel} carregando={carregando} />
                    <Indicador titulo="Receitas do mês" valor={mes?.receitas}
                        detalhe={mes ? 'Entradas confirmadas' : indisponivel} carregando={carregando} />
                    <Indicador titulo="Despesas do mês" valor={mes?.despesas}
                        detalhe={mes ? 'Pagamentos efetuados' : indisponivel} carregando={carregando} />
                    <Indicador titulo="Receita prevista" valor={recebimentos?.receitaPrevista}
                        detalhe={recebimentos ? 'Parcelas pendentes de contratos ativos' : indisponivel} carregando={carregando} />
                </section>

                <div className="dashboard-paineis">
                    <section className="dashboard-panel dashboard-fluxo" aria-labelledby="dashboard-fluxo-titulo">
                        <h2 id="dashboard-fluxo-titulo">Fluxo de caixa · últimos seis meses</h2>
                        {painel?.transacoes?.length
                            ? <>
                                <FluxoCaixa meses={meses} />
                                <p className="dashboard-caption">Receitas menos despesas por mês</p>
                            </>
                            : <p className="dashboard-empty">{carregando ? 'Carregando fluxo de caixa…'
                                : painel?.transacoes ? 'Nenhum lançamento registrado.' : indisponivel}</p>}
                    </section>
                    <section className="dashboard-panel dashboard-vencimentos" aria-labelledby="dashboard-vencimentos-titulo">
                        <h2 id="dashboard-vencimentos-titulo">Próximos vencimentos</h2>
                        {proximos?.length ? <ul>
                            {proximos.map(item => <li key={item.id}>
                                <Link to={`/app/contratos/${item.contratoId}`}>{item.clienteNome} · Parcela {item.numero}</Link>
                                <p><time dateTime={item.dataVencimento}>
                                    {dataCurta.format(new Date(`${item.dataVencimento}T00:00:00`))}
                                </time> · {moeda.format(item.valor)}</p>
                            </li>)}
                        </ul> : <p className="dashboard-empty">{carregando ? 'Carregando vencimentos…'
                            : proximos ? 'Nenhuma parcela a receber com vencimento futuro.' : indisponivel}</p>}
                    </section>
                </div>

                <section className="dashboard-acessos" aria-labelledby="dashboard-acessos-titulo">
                    <h2 id="dashboard-acessos-titulo">Acesso rápido</h2>
                    <div className="dashboard-acessos-lista">
                        <button type="button" disabled={!logado}
                            onClick={() => { setSalvo(false); setTipoModal('DESPESA'); }}>
                            <span aria-hidden="true">→</span><strong>Adicionar despesa</strong>
                        </button>
                        <div className="dashboard-acesso-inativo">
                            <span aria-hidden="true">→</span><strong>Enviar nota fiscal</strong><small>Em desenvolvimento</small>
                        </div>
                        <Link to="/app/contratos?novo=1">
                            <span aria-hidden="true">→</span><strong>Novo contrato</strong>
                        </Link>
                        <Link to="/app/orcamentos">
                            <span aria-hidden="true">→</span><strong>Ver orçamentos</strong><small>Tela em desenvolvimento</small>
                        </Link>
                    </div>
                </section>
            </div>
            {tipoModal && <LancamentoDialog tipoInicial={tipoModal} onClose={() => setTipoModal(null)} onSaved={() => {
                setTipoModal(null);
                setSalvo(true);
                setVersao(valor => valor + 1);
            }} />}
        </div>
    );
}
