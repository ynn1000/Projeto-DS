import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom';
import { Login } from './pages/login';
import { AppLayout } from './layouts/AppLayout/AppLayout';
import { ContratoDetalhes, Contratos } from './pages/contratos';

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
            { index: true, element: <Navigate to="/app/contratos" replace /> },
            { path: 'geral', element: <EmDesenvolvimento titulo="Visão geral" /> },
            { path: 'transacoes', element: <EmDesenvolvimento titulo="Transações" /> },
            { path: 'documentos', element: <EmDesenvolvimento titulo="Documentos" /> },
            { path: 'contratos', element: <Contratos /> },
            { path: 'contratos/:id', element: <ContratoDetalhes /> },
            { path: 'orcamentos', element: <EmDesenvolvimento titulo="Orçamentos" /> },
            { path: 'distribuicao', element: <EmDesenvolvimento titulo="Distribuição" /> },
            { path: 'metas', element: <EmDesenvolvimento titulo="Metas" /> },
            { path: 'equipe', element: <EmDesenvolvimento titulo="Equipe" /> },
            { path: 'perfil', element: <EmDesenvolvimento titulo="Perfil" /> },
        ],
    },
]);

export function App() {
    return <RouterProvider router={router} />;
}
