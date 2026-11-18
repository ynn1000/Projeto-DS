import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { usuarioService } from '../../services/UsuarioService';
import { getApiError } from '../../services/api';
import type { Curso, Usuario, UsuarioPayload, UsuarioUpdatePayload, RoleUsuario } from '../../types/usuario';
import './equipe.css';

const roleLabel: Record<RoleUsuario, string> = {
    ADMIN: 'Diretor',
    USER: 'Membro',
};

const cursos: { value: Curso; label: string }[] = [
    { value: 'CCOMP', label: 'Ciência da Computação' },
    { value: 'ENGCOMP', label: 'Engenharia da Computação' },
    { value: 'DESIGN', label: 'Design' },
    { value: 'OUTRO', label: 'Outro' },
];

function mensagemErro(error: unknown) {
    return getApiError(error, 'Ocorreu um erro ao processar o funcionário.');
}

function iniciais(nome: string) {
    const partes = nome.trim().split(/\s+/);
    return ((partes[0]?.[0] ?? '') + (partes[1]?.[0] ?? '')).toUpperCase();
}

export function Equipe() {
    const [usuarios, setusuarios] = useState<Usuario[]>([]);
    const [filtro, setFiltro] = useState<'' | RoleUsuario>('');
    const [carregando, setCarregando] = useState(true);
    const [erro, setErro] = useState('');
    const [modalAberto, setModalAberto] = useState(false);
    const [usuarioEmEdicao, setUsuarioEmEdicao] = useState<Usuario | null>(null);
    const [salvando, setSalvando] = useState(false);
    const [deletando, setDeletando] = useState(false);
    const [erroFormulario, setErroFormulario] = useState('');

    const carregar = useCallback(async () => {
        setCarregando(true);
        setErro('');
        try {
            setusuarios(await usuarioService.listar());
        } catch (error) {
            setErro(mensagemErro(error));
        } finally {
            setCarregando(false);
        }
    }, []);

    useEffect(() => { void carregar(); }, [carregar]);

    function selecionarFiltro(role: '' | RoleUsuario) {
        setFiltro(role);
    }

    const usuariosFiltrados = filtro ? usuarios.filter((usuario) => usuario.role === filtro) : usuarios;

    function abrirNovo() {
        setErroFormulario('');
        setUsuarioEmEdicao(null);
        setModalAberto(true);
    }

    function abrirEdicao(usuario: Usuario) {
        setErroFormulario('');
        setUsuarioEmEdicao(usuario);
        setModalAberto(true);
    }

    function fecharFormulario() {
        setModalAberto(false);
        setUsuarioEmEdicao(null);
    }

    async function salvarCom(operacao: () => Promise<Usuario>) {
        if (salvando) return;
        setSalvando(true);
        setErroFormulario('');
        try {
            await operacao();
            fecharFormulario();
            await carregar();
        } catch (error) {
            setErroFormulario(mensagemErro(error));
        } finally {
            setSalvando(false);
        }
    }

    function criarUsuario(payload: UsuarioPayload) {
        return salvarCom(() => usuarioService.criar(payload));
    }

    function atualizarUsuario(id: string, payload: UsuarioUpdatePayload) {
        return salvarCom(() => usuarioService.atualizar(id, payload));
    }

    async function excluirUsuario(id: string) {
        if (salvando || deletando) return;
        setDeletando(true);
        setErroFormulario('');
        try {
            await usuarioService.excluir(id);
            fecharFormulario();
            await carregar();
        } catch (error) {
            setErroFormulario(mensagemErro(error));
        } finally {
            setDeletando(false);
        }
    }

    return (
        <div className="team-page">
            <header className="team-header app-page-header">
                <div>
                    <h1>Equipe</h1>
                    <p className="page-header-breadcrumb">Hut8&nbsp; / &nbsp;Diretor&nbsp; / &nbsp;Colaboradores</p>
                </div>
                <button className="team-new-button page-header-action" type="button" onClick={abrirNovo}>+ Adicionar usuario</button>
            </header>

            <section className="team-content" aria-label="usuarios da equipe">
                <h2 className="team-content-title">usuarios da Hut8</h2>

                <div className="team-filter-tabs" role="tablist" aria-label="Filtrar usuarios">
                    <button
                        type="button"
                        className={filtro === '' ? 'active' : ''}
                        onClick={() => selecionarFiltro('')}
                    >
                        Todos
                    </button>
                    <button
                        type="button"
                        className={filtro === 'ADMIN' ? 'active' : ''}
                        onClick={() => selecionarFiltro('ADMIN')}
                    >
                        Diretores
                    </button>
                    <button
                        type="button"
                        className={filtro === 'USER' ? 'active' : ''}
                        onClick={() => selecionarFiltro('USER')}
                    >
                        Membros
                    </button>
                </div>

                {erro && <div className="team-feedback team-feedback-error" role="alert">{erro}</div>}
                {carregando ? <p className="team-feedback">Carregando usuarios...</p> :
                    usuariosFiltrados.length === 0 ? <div className="team-empty">Nenhum usuario encontrado.</div> :
                        <div className="team-list" aria-live="polite">
                            {usuariosFiltrados.map((usuario) => (
                                <article className="member-row" key={usuario.id}>
                                    <span className="member-avatar" aria-hidden="true">{iniciais(usuario.nome)}</span>
                                    <div className="member-identity">
                                        <strong>{usuario.nome}</strong>
                                        <span className="member-cargo">{usuario.email}</span>
                                    </div>
                                    <span className="member-squad">Matrícula: {usuario.matricula}</span>
                                    <div className="member-actions">
                                        <span className={`member-badge badge-${usuario.role.toLowerCase()}`}>{roleLabel[usuario.role]}</span>
                                        <button
                                            className="member-edit-button"
                                            type="button"
                                            aria-label={`Editar ${usuario.nome}`}
                                            onClick={() => abrirEdicao(usuario)}
                                        >
                                            Editar
                                        </button>
                                    </div>
                                </article>
                            ))}
                        </div>}
            </section>

            {modalAberto && (
                <UsuarioModal
                    key={usuarioEmEdicao?.id ?? 'novo'}
                    usuario={usuarioEmEdicao}
                    salvando={salvando}
                    deletando={deletando}
                    erro={erroFormulario}
                    onClose={() => !salvando && !deletando && fecharFormulario()}
                    onCreate={criarUsuario}
                    onUpdate={atualizarUsuario}
                    onDelete={excluirUsuario}
                />
            )}
        </div>
    );
}

interface ModalProps {
    usuario: Usuario | null;
    salvando: boolean;
    deletando: boolean;
    erro: string;
    onClose: () => void;
    onCreate: (payload: UsuarioPayload) => Promise<void>;
    onUpdate: (id: string, payload: UsuarioUpdatePayload) => Promise<void>;
    onDelete: (id: string) => Promise<void>;
}

function UsuarioModal({ usuario, salvando, deletando, erro, onClose, onCreate, onUpdate, onDelete }: ModalProps) {
    const [nome, setNome] = useState(usuario?.nome ?? '');
    const [email, setEmail] = useState(usuario?.email ?? '');
    const [senha, setSenha] = useState('');
    const [matricula, setMatricula] = useState(usuario?.matricula ?? '');
    const [curso, setCurso] = useState<Curso | ''>(usuario?.curso ?? '');
    const [role, setRole] = useState<RoleUsuario>(usuario?.role ?? 'USER');
    const [ativo, setAtivo] = useState(usuario?.ativo ?? false);
    const [semestreIngresso, setSemestreIngresso] = useState(usuario?.semestreIngresso ?? '');
    const [validacao, setValidacao] = useState('');

    function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!nome.trim()) return setValidacao('Informe o nome completo.');
        if (!email.trim()) return setValidacao('Informe o e-mail institucional.');
        if (!matricula.trim()) return setValidacao('Informe a matrícula.');
        if (!curso) return setValidacao('Informe o curso.');
        setValidacao('');

        if (usuario) {
            void onUpdate(usuario.id, {
                nome: nome.trim(),
                email: email.trim(),
                matricula: matricula.trim(),
                curso,
                ativo,
            });
            return;
        }

        if (!senha) return setValidacao('Informe a senha.');
        if (!semestreIngresso.trim()) return setValidacao('Informe o semestre de ingresso.');
        if (role === 'ADMIN' && !window.confirm('Este usuário será criado como Diretor e terá acesso de administrador. Deseja continuar?')) return;
        void onCreate({
            nome: nome.trim(),
            email: email.trim(),
            senha,
            matricula: matricula.trim(),
            curso,
            role,
            semestreIngresso: semestreIngresso.trim(),
        });
    }

    function excluir() {
        if (!usuario || salvando || deletando) return;
        if (!window.confirm(`Tem certeza que deseja excluir ${usuario.nome}? Esta ação não pode ser desfeita.`)) return;
        void onDelete(usuario.id);
    }

    return (
        <div className="member-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
            <section className="member-modal" role="dialog" aria-modal="true" aria-labelledby="member-form-title">
                <header>
                    <div>
                        <h2 id="member-form-title">{usuario ? 'Editar usuário' : 'Adicionar usuário'}</h2>
                        <p className="member-modal-lede">{usuario ? 'Atualize os dados da conta.' : 'Informe os dados necessários para vincular o usuário à Hut8.'}</p>
                    </div>
                    <button type="button" onClick={onClose} aria-label="Fechar">×</button>
                </header>

                <form onSubmit={submit}>
                    <div className="member-form-row">
                        <label>Nome completo*
                            <input type="text" required value={nome} onChange={(e) => setNome(e.target.value)} />
                        </label>
                        <label>E-mail institucional*
                            <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
                        </label>
                    </div>

                    <div className="member-form-row">
                        <label>Matrícula*
                            <input type="text" maxLength={8} required value={matricula} onChange={(e) => setMatricula(e.target.value)} />
                        </label>
                        {usuario ? (
                            <label>Curso*
                                <select required value={curso} onChange={(e) => setCurso(e.target.value as Curso | '')}>
                                    <option value="" disabled>Selecione um curso</option>
                                    {cursos.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
                                </select>
                            </label>
                        ) : (
                            <label>Senha*
                                <input type="text" autoComplete="new-password" required value={senha} onChange={(e) => setSenha(e.target.value)} />
                            </label>
                        )}
                    </div>

                    {usuario ? (
                        <div className="member-edit-meta">
                            <span>Perfil: <strong>{roleLabel[usuario.role]}</strong></span>
                            <label>
                                <input type="checkbox" checked={ativo} onChange={(event) => setAtivo(event.target.checked)} />
                                Conta ativa
                            </label>
                        </div>
                    ) : (
                        <>
                            <div className="member-form-row">
                                <label>Curso*
                                    <select required value={curso} onChange={(e) => setCurso(e.target.value as Curso | '')}>
                                        <option value="" disabled>Selecione um curso</option>
                                        {cursos.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
                                    </select>
                                </label>
                                <label>Semestre de ingresso*
                                    <input type="text" required value={semestreIngresso} onChange={(e) => setSemestreIngresso(e.target.value)} />
                                </label>
                            </div>

                            <div className="member-form-divider">
                                <h3>Acesso à Hut8</h3>
                            </div>

                            <div className="member-form-tipo">
                                <span className="member-form-tipo-label">Perfil de acesso*</span>
                                <div className="member-form-tipo-row">
                                    <div className="member-tipo-choice">
                                        <button
                                            type="button"
                                            className={role === 'USER' ? 'active' : ''}
                                            onClick={() => setRole('USER')}
                                        >
                                            Membro
                                        </button>
                                        <button
                                            type="button"
                                            className={role === 'ADMIN' ? 'active' : ''}
                                            onClick={() => setRole('ADMIN')}
                                        >
                                            <span className="member-tipo-radio" aria-hidden="true" />
                                            Diretor (Administrador)
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </>
                    )}

                    {(validacao || erro) && <p className="team-feedback team-feedback-error" role="alert">{validacao || erro}</p>}

                    <footer>
                        <div className="member-form-footer-start">
                            <span className="member-form-required">* Campos obrigatórios</span>
                            {usuario && (
                                <button
                                    type="button"
                                    className="member-delete-button"
                                    onClick={excluir}
                                    disabled={salvando || deletando}
                                >
                                    {deletando ? 'Excluindo...' : 'Excluir usuário'}
                                </button>
                            )}
                        </div>
                        <div className="member-form-actions">
                            <button type="button" className="member-cancel-button" onClick={onClose} disabled={salvando || deletando}>Cancelar</button>
                            <button type="submit" className="team-new-button" disabled={salvando || deletando}>
                                {salvando ? 'Salvando...' : usuario ? 'Salvar alterações' : 'Adicionar usuário'}
                            </button>
                        </div>
                    </footer>
                </form>
            </section>
        </div>
    );
}