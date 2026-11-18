import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom';
import { Login } from './pages/login';
import { Dashboard } from './pages/dashboard';
import { Equipe } from './pages/equipe'
import { AppLayout } from './layouts/AppLayout/AppLayout';
import { ContratoDetalhes, Contratos } from './pages/contratos';
import { Orcamentos } from './pages/solicitar_orcamento/Orcamentos';
import { OrcamentoDetalhes } from './pages/solicitar_orcamento/OrcamentoDetalhes';
import { OrcamentosLayout } from './pages/solicitar_orcamento/OrcamentosLayout';
import { SolicitarOrcamento } from './pages/solicitar_orcamento';
import { NovaTransacao, Transacoes } from './pages/transacoes'

function EmDesenvolvimento({ titulo }: { titulo: string }) {
    return (
        <section style={{ padding: '40px 48px' }}>
            <h1>{titulo}</h1>
            <p>Esta página será implementada em uma próxima etapa.</p>
        </section>
    );
}

const router = createBrowserRouter([
    { path: '/', element: <Navigate to="/auth/login" replace /> },
    { path: '/auth/login', element: <Login /> },
    {
        path: '/app',
        element: <AppLayout />,
        children: [
            { index: true, element: <Navigate to="/app/geral" replace /> },
            { path: 'geral', element: <Dashboard /> },
            {
                path: 'transacoes',
                children: [
                    { index: true, element: <Transacoes /> },
                    { path: 'nova', element: <NovaTransacao /> },
                ],
            },
            { path: 'documentos', element: <EmDesenvolvimento titulo="Documentos" /> },
            { path: 'contratos', element: <Contratos /> },
            { path: 'contratos/:id', element: <ContratoDetalhes /> },
            {
                path: 'orcamentos',
                element: <OrcamentosLayout />,
                children: [
                    { index: true, element: <Orcamentos /> },
                    { path: 'solicitar', element: <SolicitarOrcamento /> },
                    { path: ':id', element: <OrcamentoDetalhes /> },
                ],
            },
            { path: 'distribuicao', element: <EmDesenvolvimento titulo="Distribuição" /> },
            { path: 'metas', element:  <EmDesenvolvimento titulo="Metas" /> },
            { path: 'equipe', element: <Equipe /> },
            { path: 'perfil', element: <EmDesenvolvimento titulo="Perfil" /> },
        ],
    },
]);

export function App() {
    return <RouterProvider router={router} />;
}
