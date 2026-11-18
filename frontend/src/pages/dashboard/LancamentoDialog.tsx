import { useEffect, useRef, useState, type FormEvent } from 'react';
import { getApiError } from '../../services/api';
import {
    categoriasTransacao, dashboardService, type CategoriaTransacao,
    type OpcoesLancamento, type TipoTransacao,
} from '../../services/dashboardService';
import { dataLocalISO, moeda } from './financeiro';

const VALOR_MAXIMO_CENTAVOS = 999_999_999;

function posicionarCursorNoFim(campo: HTMLInputElement) {
    if (campo.selectionStart !== campo.selectionEnd) return;
    campo.setSelectionRange(campo.value.length, campo.value.length);
}

interface Props {
    tipoInicial: TipoTransacao;
    onClose: () => void;
    onSaved: () => void;
}

export function LancamentoDialog({ tipoInicial, onClose, onSaved }: Props) {
    const dialog = useRef<HTMLDialogElement>(null);
    const enviando = useRef(false);
    const [tipo, setTipo] = useState<TipoTransacao>(tipoInicial);
    const [data, setData] = useState(dataLocalISO);
    const [valorCentavos, setValorCentavos] = useState(0);
    const [verba, setVerba] = useState('');
    const [subverba, setSubverba] = useState('');
    const [salvando, setSalvando] = useState(false);
    const [erro, setErro] = useState('');
    const [resultado, setResultado] = useState<{ data: string; opcoes?: OpcoesLancamento; erro?: string }>();

    useEffect(() => {
        const elemento = dialog.current;
        elemento?.showModal();
        return () => { elemento?.close(); };
    }, []);

    useEffect(() => {
        if (tipo !== 'DESPESA' || !data) return;
        const controller = new AbortController();
        void dashboardService.opcoes(data, controller.signal).then(
            opcoes => { if (!controller.signal.aborted) setResultado({ data, opcoes }); },
            falha => { if (!controller.signal.aborted) setResultado({ data, erro: getApiError(falha) }); },
        );
        return () => controller.abort();
    }, [data, tipo]);

    const atual = resultado?.data === data ? resultado : undefined;
    const verbas = atual?.opcoes?.verbas ?? [];
    const subverbas = atual?.opcoes?.subverbas.filter(opcao => opcao.categoriaOrcamentariaId === Number(verba)) ?? [];
    const despesa = tipo === 'DESPESA';
    const carregandoVerbas = despesa && Boolean(data) && !atual;

    async function salvar(evento: FormEvent<HTMLFormElement>) {
        evento.preventDefault();
        if (enviando.current || (despesa && !verbas.some(opcao => opcao.id === Number(verba)))) return;
        const dados = new FormData(evento.currentTarget);
        const descricao = String(dados.get('descricao') ?? '').trim();
        if (!descricao || valorCentavos <= 0) {
            setErro('Informe a descrição e um valor maior que zero.');
            return;
        }
        enviando.current = true;
        setSalvando(true);
        setErro('');
        try {
            await dashboardService.registrar({
                descricao, tipo, valor: valorCentavos / 100,
                categoriaTransacao: dados.get('categoriaTransacao') as CategoriaTransacao,
                dataMovimento: data,
                categoriaOrcamentariaId: despesa ? Number(verba) : null,
                subverbaId: despesa && subverbas.some(opcao => opcao.id === Number(subverba)) ? Number(subverba) : null,
            });
            onSaved();
        } catch (falha) {
            setErro(getApiError(falha, 'Não foi possível salvar o lançamento.'));
        } finally {
            enviando.current = false;
            setSalvando(false);
        }
    }

    return (
        <dialog ref={dialog} className="dashboard-dialog" aria-labelledby="dashboard-dialog-titulo"
            onCancel={evento => { evento.preventDefault(); if (!salvando) onClose(); }}>
            <form onSubmit={salvar}>
                <header>
                    <div>
                        <h2 id="dashboard-dialog-titulo">Novo lançamento</h2>
                        <p>Registre uma receita ou despesa no Caixa.</p>
                    </div>
                    <button type="button" className="dashboard-dialog-close" aria-label="Fechar"
                        disabled={salvando} onClick={onClose}>×</button>
                </header>
                <fieldset disabled={salvando}>
                    <label>Descrição <input name="descricao" required maxLength={255} /></label>
                    <div className="dashboard-dialog-fields">
                        <label>Tipo
                            <select value={tipo} onChange={evento => {
                                setTipo(evento.target.value as TipoTransacao);
                                setVerba('');
                                setSubverba('');
                            }}>
                                <option value="DESPESA">Despesa</option>
                                <option value="RECEITA">Receita</option>
                            </select>
                        </label>
                        <label>Categoria da transação
                            <select name="categoriaTransacao" defaultValue="OUTROS">
                                {Object.entries(categoriasTransacao).map(([chave, nome]) =>
                                    <option key={chave} value={chave}>{nome}</option>)}
                            </select>
                        </label>
                        <label>Valor (R$)
                            <input type="text" name="valor" inputMode="numeric" autoComplete="off" required
                                className="dashboard-valor" value={moeda.format(valorCentavos / 100)}
                                onFocus={evento => posicionarCursorNoFim(evento.currentTarget)}
                                onClick={evento => posicionarCursorNoFim(evento.currentTarget)}
                                onChange={evento => {
                                    const centavos = Number(evento.currentTarget.value.replace(/\D/g, ''));
                                    if (centavos <= VALOR_MAXIMO_CENTAVOS) setValorCentavos(centavos);
                                }} />
                        </label>
                        <label>Data do movimento
                            <input type="date" required value={data} onChange={evento => {
                                setData(evento.target.value);
                                setSubverba('');
                            }} />
                        </label>
                        {despesa && <>
                            <label>Verba orçamentária
                                <select required value={verba} disabled={carregandoVerbas || !verbas.length}
                                    onChange={evento => { setVerba(evento.target.value); setSubverba(''); }}>
                                    <option value="">{carregandoVerbas ? 'Carregando…' : 'Selecione uma verba'}</option>
                                    {verbas.map(opcao => <option key={opcao.id} value={opcao.id}>{opcao.nome}</option>)}
                                </select>
                            </label>
                            <label>Subverba (opcional)
                                <select value={subverba} disabled={!subverbas.length}
                                    onChange={evento => setSubverba(evento.target.value)}>
                                    <option value="">Sem subverba</option>
                                    {subverbas.map(opcao => <option key={opcao.id} value={opcao.id}>
                                        {opcao.nome} · {opcao.periodo}
                                    </option>)}
                                </select>
                            </label>
                        </>}
                    </div>
                </fieldset>
                {despesa && atual?.erro && <p className="dashboard-message" role="alert">{atual.erro}</p>}
                {despesa && atual?.opcoes && !verbas.length &&
                    <p className="dashboard-message">Não há verbas orçamentárias ativas cadastradas.</p>}
                {erro && <p className="dashboard-message" role="alert">{erro}</p>}
                <footer>
                    <button type="button" className="dashboard-secondary" disabled={salvando} onClick={onClose}>Cancelar</button>
                    <button type="submit" className="dashboard-primary" disabled={salvando || (despesa && !verbas.length)}>
                        {salvando ? 'Salvando…' : 'Salvar lançamento'}
                    </button>
                </footer>
            </form>
        </dialog>
    );
}
