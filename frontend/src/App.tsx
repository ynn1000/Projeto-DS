import { createBrowserRouter, RouterProvider } from 'react-router'
import { Login } from './pages/login'

const router = createBrowserRouter([
    {
        path: '/auth/login',
        element: <Login />
    }
])

export function App() {
    return <RouterProvider router={router} />
}