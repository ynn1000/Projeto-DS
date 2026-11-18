import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getApiError } from '../../services/api';
import { transacaoService } from '../../services/transacaoService';
import type {
    CategoriaOrcamentaria, CategoriaTransacao, PeriodoOrcamento, PrestacaoOpcao,
    ProjetoOpcao, Subverba, TipoTransacao, TransacaoCreate,
} from '../../types/transacao';
import './transacoes.css';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const hojeLocal = () => {
    const hoje = new Date();
    return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}-${String(hoje.getDate()).padStart(2, '0')}`;
};
const formatarCentavos = (centavos: number) => (centavos / 100).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const aplicarMascaraMoeda = (valor: string) => {
    const digitos = valor.replace(/\D/g, '').slice(0, 10);
    return digitos ? formatarCentavos(Number(digitos)) : '';
};
const converterMoeda = (valor: string) => Number(valor.replace(/\./g, '').replace(',', '.'));
const categoriaLabel: Record<CategoriaTransacao, string> = {
    VENDA: 'Venda', EMPRESTIMO: 'Empréstimo', ALIMENTOS: 'Alimentos', OUTROS: 'Outros',
};

export function NovaTransacao() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const prestacaoInicial = searchParams.get('prestacaoId') ?? '';
    const [tipo, setTipo] = useState<TipoTransacao>('DESPESA');
    const [descricao, setDescricao] = useState('');
    const [valor, setValor] = useState('');
    const [dataMovimento, setDataMovimento] = useState(hojeLocal);
    const [categoriaTransacao, setCategoriaTransacao] = useState<CategoriaTransacao | ''>('');
    const [categoriaOrcamentariaId, setCategoriaOrcamentariaId] = useState('');
    const [periodoId, setPeriodoId] = useState('');
    const [subverbaId, setSubverbaId] = useState('');
    const [projetoId, setProjetoId] = useState('');
    const [prestacaoId, setPrestacaoId] = useState(prestacaoInicial);
    const [categorias, setCategorias] = useState<CategoriaOrcamentaria[]>([]);
    const [periodos, setPeriodos] = useState<PeriodoOrcamento[]>([]);
    const [subverbas, setSubverbas] = useState<Subverba[]>([]);
    const [projetos, setProjetos] = useState<ProjetoOpcao[]>([]);
    const [prestacoes, setPrestacoes] = useState<PrestacaoOpcao[]>([]);
    const [carregandoOpcoes, setCarregandoOpcoes] = useState(true);
    const [carregandoSubverbas, setCarregandoSubverbas] = useState(false);
    const [salvando, setSalvando] = useState(false);
    const [erroOpcoes, setErroOpcoes] = useState('');
    const [erroSubverbas, setErroSubverbas] = useState('');
    const [erro, setErro] = useState('');
    const [validacao, setValidacao] = useState('');

    useEffect(() => {
        let ativo = true;
        Promise.allSettled([
            transacaoService.listarCategoriasOrcamentarias(),
            transacaoService.listarPeriodosOrcamentoAtivos(),
            transacaoService.listarProjetosAtivos(),
            transacaoService.listarPrestacoesPendentes(),
        ]).then(([categoriasResult, periodosResult, projetosResult, prestacoesResult]) => {
            if (!ativo) return;
            const mensagens: string[] = [];
            if (categoriasResult.status === 'fulfilled') setCategorias(categoriasResult.value);
            else mensagens.push(`Categorias: ${getApiError(categoriasResult.reason, 'não foi possível carregar.')}`);
            if (periodosResult.status === 'fulfilled') setPeriodos(periodosResult.value);
            else mensagens.push(`Períodos: ${getApiError(periodosResult.reason, 'não foi possível carregar.')}`);
            if (projetosResult.status === 'fulfilled') setProjetos(projetosResult.value);
            else mensagens.push(`Projetos: ${getApiError(projetosResult.reason, 'não foi possível carregar.')}`);
            if (prestacoesResult.status === 'fulfilled') {
                setPrestacoes(prestacoesResult.value);
                if (prestacaoInicial) {
                    const selecionada = prestacoesResult.value.find((item) => String(item.id) === prestacaoInicial);
                    if (selecionada) {
                        setPrestacaoId(String(selecionada.id)); setTipo('RECEITA');
                        setProjetoId(String(selecionada.projetoId));
                        setValor(selecionada.valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
                        setDescricao(`Recebimento da prestação ${selecionada.numero} - ${selecionada.clienteNome}`);
                    }
                }
            } else mensagens.push(`Prestações: ${getApiError(prestacoesResult.reason, 'não foi possível carregar.')}`);
            setErroOpcoes(mensagens.join(' '));
        }).finally(() => { if (ativo) setCarregandoOpcoes(false); });
        return () => { ativo = false; };
    }, [prestacaoInicial]);

    const periodosDaData = useMemo(
        () => periodos.filter((periodo) => dataMovimento >= periodo.dataInicio && dataMovimento <= periodo.dataFim),
        [periodos, dataMovimento],
    );
    const periodoAtual = periodosDaData.find((periodo) => String(periodo.id) === periodoId);

    useEffect(() => {
        if (tipo !== 'DESPESA' || !categoriaOrcamentariaId || !periodoAtual) return;
        let ativo = true;
        transacaoService.listarSubverbas(periodoAtual.id, Number(categoriaOrcamentariaId))
            .then((lista) => { if (ativo) setSubverbas(lista); })
            .catch((error: unknown) => { if (ativo) { setSubverbas([]); setErroSubverbas(getApiError(error, 'Não foi possível carregar as subverbas.')); } })
            .finally(() => { if (ativo) setCarregandoSubverbas(false); });
        return () => { ativo = false; };
    }, [tipo, categoriaOrcamentariaId, periodoAtual]);

    function alterarTipo(novoTipo: TipoTransacao) {
        setTipo(novoTipo); setPrestacaoId(''); setValidacao(''); setErro('');
        if (novoTipo === 'RECEITA') {
            setCategoriaOrcamentariaId(''); setPeriodoId(''); setSubverbaId(''); setSubverbas([]);
            setCarregandoSubverbas(false); setErroSubverbas('');
        }
    }
    function selecionarPrestacao(value: string) {
        setPrestacaoId(value);
        const prestacao = prestacoes.find((item) => String(item.id) === value);
        if (prestacao) {
            setTipo('RECEITA'); setProjetoId(String(prestacao.projetoId));
            setValor(prestacao.valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
        }
    }
    function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (salvando) return;
        setErro(''); setValidacao('');
        const valorNumerico = converterMoeda(valor);
        if (!descricao.trim()) return setValidacao('Informe a descrição da transação.');
        if (descricao.trim().length > 255) return setValidacao('A descrição deve ter no máximo 255 caracteres.');
        if (!categoriaTransacao) return setValidacao('Selecione a categoria da transação.');
        if (!dataMovimento) return setValidacao('Informe a data da movimentação.');
        if (!valor || !Number.isFinite(valorNumerico) || valorNumerico <= 0 || valorNumerico > 9999999.99) return setValidacao('Informe um valor positivo, de até R$ 9.999.999,99.');
        if (!/^\d{1,3}(?:\.\d{3})*(?:,\d{2})$/.test(valor)) return setValidacao('Informe o valor com duas casas decimais.');
        if (tipo === 'DESPESA' && !categoriaOrcamentariaId) return setValidacao('Selecione a categoria orçamentária da despesa.');
        if (subverbaId && !periodoAtual) return setValidacao('A data informada não pertence ao período de orçamento selecionado.');
        const payload: TransacaoCreate = {
            descricao: descricao.trim(), tipo, categoriaTransacao, valor: valorNumerico, dataMovimento,
            categoriaOrcamentariaId: tipo === 'DESPESA' ? Number(categoriaOrcamentariaId) : null,
            subverbaId: tipo === 'DESPESA' && subverbaId ? Number(subverbaId) : null,
            projetoId: projetoId ? Number(projetoId) : null,
            prestacaoId: prestacaoId ? Number(prestacaoId) : null,
        };
        setSalvando(true);
        transacaoService.criar(payload)
            .then(() => navigate('/app/transacoes', { replace: true }))
            .catch((error: unknown) => setErro(getApiError(error, 'Não foi possível registrar a transação.')))
            .finally(() => setSalvando(false));
    }

    return (
        <div className="transactions-page">
            <header className="transactions-header">
                <div><h1>Nova transação</h1><p className="transactions-breadcrumb">Hut8&nbsp; / &nbsp;Financeiro&nbsp; / &nbsp;Cadastrar receita ou despesa</p></div>
            </header>
            <main className="transactions-content transactions-form-content">
                <section className="transaction-form-card">
                    <h2>Registrar movimentação</h2>
                    <form onSubmit={submit}>
                        <fieldset className="transaction-type-choice">
                            <legend>Tipo de movimentação</legend>
                            <label className={tipo === 'DESPESA' ? 'selected' : ''}><input type="radio" name="tipo" value="DESPESA" checked={tipo === 'DESPESA'} onChange={() => alterarTipo('DESPESA')} />Despesa</label>
                            <label className={tipo === 'RECEITA' ? 'selected' : ''}><input type="radio" name="tipo" value="RECEITA" checked={tipo === 'RECEITA'} onChange={() => alterarTipo('RECEITA')} />Receita</label>
                        </fieldset>
                        <label className="transaction-field">Descrição da transação<input type="text" maxLength={255} required placeholder="Ex.: Licença para software" value={descricao} onChange={(event) => setDescricao(event.target.value)} /></label>
                        <div className="transaction-form-row">
                            <label className="transaction-field">Valor (R$)<input type="text" inputMode="numeric" required placeholder="0,00" value={valor} onChange={(event) => setValor(aplicarMascaraMoeda(event.target.value))} /></label>
                            <label className="transaction-field">Data<input type="date" required value={dataMovimento} onChange={(event) => { setDataMovimento(event.target.value); setPeriodoId(''); setSubverbaId(''); setSubverbas([]); }} /></label>
                        </div>
                        <label className="transaction-field">Categoria da transação
                            <select required value={categoriaTransacao} onChange={(event) => setCategoriaTransacao(event.target.value as CategoriaTransacao | '')}>
                                <option value="">Selecione uma categoria</option>
                                {Object.entries(categoriaLabel).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                            </select>
                        </label>
                        <label className="transaction-field">Projeto <span className="optional-label">Opcional</span>
                            <select value={projetoId} disabled={Boolean(prestacaoId)} onChange={(event) => setProjetoId(event.target.value)}>
                                <option value="">Sem vínculo com projeto</option>
                                {projetos.map((projeto) => <option key={projeto.id} value={projeto.id}>{projeto.nome}</option>)}
                            </select>
                            {prestacaoId && <small className="transaction-field-hint">O projeto é definido automaticamente pelo contrato da prestação.</small>}
                        </label>
                        {tipo === 'RECEITA' && <label className="transaction-field">Prestação de contrato <span className="optional-label">Opcional</span>
                            <select value={prestacaoId} onChange={(event) => selecionarPrestacao(event.target.value)}>
                                <option value="">Receita sem prestação</option>
                                {prestacoes.map((prestacao) => <option key={prestacao.id} value={prestacao.id}>#{prestacao.numero} · {prestacao.projetoNome} · {prestacao.clienteNome} · {moeda.format(Number(prestacao.valor))} · vence {new Date(`${prestacao.dataVencimento}T00:00:00`).toLocaleDateString('pt-BR')}</option>)}
                            </select>
                            {prestacaoId && <small className="transaction-field-hint">O backend exige o valor integral da prestação e marcará a parcela como paga ao registrar a receita.</small>}
                        </label>}
                        {tipo === 'DESPESA' && <>
                            <label className="transaction-field">Categoria orçamentária
                                <select required disabled={carregandoOpcoes || Boolean(erroOpcoes)} value={categoriaOrcamentariaId} onChange={(event) => { setCategoriaOrcamentariaId(event.target.value); setPeriodoId(''); setSubverbaId(''); setSubverbas([]); }}>
                                    <option value="">{carregandoOpcoes ? 'Carregando categorias...' : 'Selecione uma categoria'}</option>
                                    {categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nome}</option>)}
                                </select>
                            </label>
                            {categoriaOrcamentariaId && <>
                                <label className="transaction-field">Período do orçamento
                                    <select value={periodoId} onChange={(event) => { setPeriodoId(event.target.value); setSubverbaId(''); setSubverbas([]); setErroSubverbas(''); setCarregandoSubverbas(Boolean(event.target.value)); }}>
                                        <option value="">Selecione um período (opcional)</option>
                                        {periodosDaData.map((periodo) => <option key={periodo.id} value={periodo.id}>{periodo.nome} ({new Date(`${periodo.dataInicio}T00:00:00`).toLocaleDateString('pt-BR')} – {new Date(`${periodo.dataFim}T00:00:00`).toLocaleDateString('pt-BR')})</option>)}
                                    </select>
                                    {periodosDaData.length === 0 && <small className="transaction-field-hint">Não há período ativo que inclua a data selecionada. A despesa pode ser registrada sem subverba.</small>}
                                </label>
                                {periodoAtual && <label className="transaction-field">Subverba <span className="optional-label">Opcional</span>
                                    <select value={subverbaId} onChange={(event) => setSubverbaId(event.target.value)} disabled={carregandoSubverbas}>
                                        <option value="">{carregandoSubverbas ? 'Carregando subverbas...' : 'Sem subverba'}</option>
                                        {subverbas.map((subverba) => <option key={subverba.id} value={subverba.id}>{subverba.nome} · saldo {moeda.format(Number(subverba.saldoSubverba))}</option>)}
                                    </select>
                                    {erroSubverbas && <small className="transaction-inline-error">{erroSubverbas}</small>}
                                </label>}
                            </>}
                        </>}
                        {erroOpcoes && <p className="transactions-feedback transactions-error" role="alert">{erroOpcoes}</p>}
                        {validacao && <p className="transactions-feedback transactions-error" role="alert">{validacao}</p>}
                        {erro && <p className="transactions-feedback transactions-error" role="alert">{erro}</p>}
                        <footer className="transaction-form-actions">
                            <button type="button" className="transaction-cancel-button" disabled={salvando} onClick={() => navigate('/app/transacoes')}>Cancelar</button>
                            <button type="submit" className="transactions-new-button" disabled={salvando || carregandoOpcoes}>{salvando ? 'Salvando...' : 'Salvar transação'}</button>
                        </footer>
                    </form>
                </section>
            </main>
        </div>
    );
}
