import { Outlet } from "react-router-dom";
import { Sidebar } from "../../components/Sidebar/Sidebar";
import "./app-layout.css";

export function AppLayout() {
    return (
        <div className="app-layout">
            <Sidebar />

            <main className="app-content">
                <Outlet />
            </main>
        </div>
    );
}