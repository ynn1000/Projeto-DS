import { createBrowserRouter, RouterProvider } from 'react-router'

const router = createBrowserRouter([
  {
    path: '/',
    element: <h1>FinHut8</h1>,
  }
])

export function App() {
  return <RouterProvider router={router}/>
} 