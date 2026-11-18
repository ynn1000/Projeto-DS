import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { getApiError } from '../../services/api';
import { orcamentoService } from '../../services/orcamentoService';
import type { RoleUsuario } from '../../types/usuario';

export function OrcamentosLayout() {
    const [perfil, setPerfil] = useState<RoleUsuario>();
    const [erro, setErro] = useState('');
    const [tentativa, setTentativa] = useState(0);

    useEffect(() => {
        let ativo = true;
        orcamentoService.consultarPerfil()
            .then((role) => { if (ativo) setPerfil(role); })
            .catch((error: unknown) => {
                if (ativo) setErro(getApiError(error, 'Não foi possível consultar seu perfil.'));
            });
        return () => { ativo = false; };
    }, [tentativa]);

    if (perfil) return <Outlet context={perfil} />;

    return (
        <div className="budget-request-content">
            {!erro ? <p role="status">Carregando orçamentos…</p> : (
                <div className="budget-feedback budget-error">
                    <span role="alert">{erro}</span>{' '}
                    <button type="button" onClick={() => {
                        setErro('');
                        setTentativa((atual) => atual + 1);
                    }}>Tentar novamente</button>
                </div>
            )}
        </div>
    );
}
