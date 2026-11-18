export type RoleUsuario = 'ADMIN' | 'USER';
export type Curso = 'CCOMP' | 'ENGCOMP' | 'DESIGN' | 'OUTRO';

export interface Usuario {
    id: string;
    nome: string;
    email: string;
    ativo: boolean;
    dataCriacao: string;
    matricula: string;
    curso: Curso;
    role: RoleUsuario;
    semestreIngresso: string;
}

export interface UsuarioPayload {
    nome: string;
    email: string;
    senha: string;
    matricula: string;
    curso: Curso;
    role: RoleUsuario;
    semestreIngresso: string;
}

export interface UsuarioUpdatePayload {
    nome?: string;
    senha?: string;
    email?: string;
    matricula?: string;
    curso?: Curso;
    ativo?: boolean;
}