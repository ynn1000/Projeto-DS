import { createBrowserRouter, RouterProvider } from 'react-router'
import { Login } from './pages/login'
import { AppLayout } from './layouts/AppLayout/AppLayout'

const router = createBrowserRouter([
    {
        path: '/auth/login',
        element: <Login />
    },
    {
        path: '/app',
        element: <AppLayout />,
        children: [
           /*{
                path: "/geral",
                element: 
            },
            {
                path: "/transacoes",
                element:
            },
            {
                path: "/documentos",
                element:
            },
            {
                path: "/contratos",
                element:
            },
            {
                path: "/orcamentos",
                element:
            },
            {
                path: "/metas",
                element: 
            },
            {
                path: "/equipe",
                element: 
            },
            {
                path: "/perfil",
                element:
            }*/
        ]
    }
    
])

export function App() {
    return <RouterProvider router={router} />
}