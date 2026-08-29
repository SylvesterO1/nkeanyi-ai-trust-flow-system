import { Navigate, Route, Routes } from "react-router-dom";
import "./App.css";
import { AppShell } from "./components/AppShell";
import { AuthDebugPage } from "./pages/AuthDebugPage";
import { BillingPage } from "./pages/BillingPage";
import { DashboardPage } from "./pages/DashboardPage";
import { DemoScriptPage } from "./pages/DemoScriptPage";
import { DocumentsPage } from "./pages/DocumentsPage";
import { EvidencePage } from "./pages/EvidencePage";
import { HealthPage } from "./pages/HealthPage";
import { KnowledgePage } from "./pages/KnowledgePage";
import { PaymentsPage } from "./pages/PaymentsPage";
import { TrustFlowDemoPage } from "./pages/TrustFlowDemoPage";

function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/auth-debug" element={<AuthDebugPage />} />
        <Route path="/demo-script" element={<DemoScriptPage />} />
        <Route path="/billing" element={<BillingPage />} />
        <Route path="/documents" element={<DocumentsPage />} />
        <Route path="/knowledge" element={<KnowledgePage />} />
        <Route path="/health" element={<HealthPage />} />
        <Route path="/evidence" element={<EvidencePage />} />
        <Route path="/payments" element={<PaymentsPage />} />
        <Route path="/trust-flow-demo" element={<TrustFlowDemoPage />} />
      </Routes>
    </AppShell>
  );
}

export default App;
