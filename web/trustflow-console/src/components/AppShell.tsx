import type { ReactNode } from "react";
import { Header } from "./Header";
import { Sidebar } from "./Sidebar";

type AppShellProps = {
  children: ReactNode;
};

export function ProductShell({ children }: AppShellProps) {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-workspace">
        <Header />
        <main className="main-content">{children}</main>
      </div>
    </div>
  );
}

export function AppShell({ children }: AppShellProps) {
  return <ProductShell>{children}</ProductShell>;
}
