import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import type { RoleUsuario } from '../../types/usuario';
import { getApiError } from '../../services/api';
import { orcamentoService } from '../../services/orcamentoService';
import type { CategoriaOrcamentaria } from '../../types/transacao';
import './solicitar_orcamento.css';

const TITULO_MAX = 150;
const VALOR_MAX = 9999999.99;

const formatarCentavos = (centavos: number) => (centavos / 100).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const aplicarMascaraMoeda = (valor: string) => {
    const digitos = valor.replace(/\D/g, '').slice(0, 9);
    return digitos ? formatarCentavos(Number(digitos)) : '';
};
const converterMoeda = (valor: string) => Number(valor.replace(/\./g, '').replace(',', '.'));

export function SolicitarOrcamento() {
    const navigate = useNavigate();
    const perfil = useOutletContext<RoleUsuario>();
    const [categorias, setCategorias] = useState<CategoriaOrcamentaria[]>([]);
    const [carregandoCategorias, setCarregandoCategorias] = useState(true);
    const [erroCategorias, setErroCategorias] = useState('');
    const [tentativaCategorias, setTentativaCategorias] = useState(0);
    const [titulo, setTitulo] = useState('');
    const [categoriaId, setCategoriaId] = useState('');
    const [valor, setValor] = useState('');
    const [descricao, setDescricao] = useState('');
    const [arquivoRef, setArquivoRef] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [validacao, setValidacao] = useState('');
    const [erro, setErro] = useState('');

    useEffect(() => {
        let ativo = true;
        orcamentoService.listarCategorias()
            .then((lista) => { if (ativo) setCategorias(lista); })
            .catch((error: unknown) => { if (ativo) setErroCategorias(getApiError(error, 'Não foi possível carregar as categorias.')); })
            .finally(() => { if (ativo) setCarregandoCategorias(false); });
        return () => { ativo = false; };
    }, [tentativaCategorias]);

    function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (enviando) return;
        setValidacao('');
        setErro('');

        const valorNumerico = converterMoeda(valor);
        if (!titulo.trim()) return setValidacao('Informe o título da solicitação.');
        if (titulo.trim().length > TITULO_MAX) return setValidacao(`O título deve ter no máximo ${TITULO_MAX} caracteres.`);
        if (!categoriaId) return setValidacao('Selecione a categoria.');
        if (!valor || !Number.isFinite(valorNumerico) || valorNumerico <= 0 || valorNumerico > VALOR_MAX) return setValidacao('Informe um valor maior que zero, de até R$ 9.999.999,99.');
        if (!descricao.trim()) return setValidacao('Explique a necessidade na justificativa.');

        setEnviando(true);
        orcamentoService.solicitar({
            categoriaId: Number(categoriaId),
            titulo: titulo.trim(),
            descricao: descricao.trim(),
            valorSolicitado: valorNumerico,
            arquivoRef: arquivoRef.trim() || null,
        })
            .then((pedido) => navigate('/app/orcamentos', {
                replace: true,
                state: { pedidoCriado: pedido.id },
            }))
            .catch((error: unknown) => setErro(getApiError(error, 'Não foi possível enviar a solicitação.')))
            .finally(() => setEnviando(false));
    }

    return (
        <div className="budget-request-page">
            <header className="app-page-header">
                <div>
                    <h1>Solicitar orçamento</h1>
                    <p className="page-header-breadcrumb">Hut8 / {perfil === 'ADMIN' ? 'Diretor' : 'Colaborador'} / Nova solicitação</p>
                </div>
            </header>

            <div className="budget-request-content">
                <section className="budget-request-card">
                    <h2>Nova solicitação de orçamento</h2>
                    <form onSubmit={submit}>
                        <label className="budget-field">Título da solicitação
                            <input type="text" maxLength={TITULO_MAX} required placeholder="Ex.: Curso de desenvolvimento web"
                                value={titulo} onChange={(event) => setTitulo(event.target.value)} />
                        </label>

                        <div className="budget-form-row">
                            <label className="budget-field">Categoria
                                <select required disabled={carregandoCategorias || Boolean(erroCategorias) || !categorias.length} value={categoriaId}
                                    onChange={(event) => setCategoriaId(event.target.value)}>
                                    <option value="">{carregandoCategorias ? 'Carregando categorias...' : 'Selecione uma categoria'}</option>
                                    {categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nome}</option>)}
                                </select>
                            </label>
                            <label className="budget-field">Valor estimado (R$)
                                <input type="text" inputMode="numeric" required placeholder="0,00"
                                    value={valor} onChange={(event) => setValor(aplicarMascaraMoeda(event.target.value))} />
                            </label>
                        </div>

                        <label className="budget-field">Justificativa / descrição
                            <textarea rows={5} required placeholder="Explique a necessidade para a equipe..."
                                value={descricao} onChange={(event) => setDescricao(event.target.value)} />
                        </label>

                        <label className="budget-field"><span>Link do anexo <span className="optional-label">Opcional</span></span>
                            <input type="text" maxLength={4000} className="budget-attachment"
                                placeholder="Cole o link da proposta ou do comprovante"
                                value={arquivoRef} onChange={(event) => setArquivoRef(event.target.value)} />
                        </label>

                        {erroCategorias && (
                            <div className="budget-feedback budget-error">
                                <span role="alert">{erroCategorias}</span>{' '}
                                <button type="button" onClick={() => {
                                    setErroCategorias('');
                                    setCarregandoCategorias(true);
                                    setTentativaCategorias((tentativa) => tentativa + 1);
                                }}>Tentar novamente</button>
                            </div>
                        )}
                        {!carregandoCategorias && !erroCategorias && !categorias.length && (
                            <p role="status">Nenhuma categoria orçamentária ativa está disponível.</p>
                        )}
                        {validacao && <p className="budget-feedback budget-error" role="alert">{validacao}</p>}
                        {erro && <p className="budget-feedback budget-error" role="alert">{erro}</p>}

                        <footer className="budget-form-actions">
                            <button type="button" className="budget-cancel" disabled={enviando} onClick={() => navigate('/app/orcamentos', { replace: true })}>Cancelar</button>
                            <button type="submit" className="budget-submit" disabled={enviando || carregandoCategorias || Boolean(erroCategorias) || !categorias.length}>
                                {enviando ? 'Enviando...' : 'Enviar solicitação'}
                            </button>
                        </footer>
                    </form>
                </section>
            </div>
        </div>
    );
}
