import {
  Activity,
  BarChart3,
  BookOpenText,
  ClipboardList,
  CreditCard,
  FileText,
  Landmark,
  LayoutDashboard,
  Network,
  ReceiptText,
  ShieldCheck,
} from "lucide-react";
import { NavLink } from "react-router-dom";

const navSections = [
  {
    label: "Overview",
    items: [
      { icon: LayoutDashboard, label: "Dashboard", to: "/dashboard" },
      { icon: Network, label: "Trust Flow Demo", to: "/trust-flow-demo" },
    ],
  },
  {
    label: "Workflows",
    items: [
      { icon: FileText, label: "Documents", to: "/documents" },
      { icon: BookOpenText, label: "Knowledge", to: "/knowledge" },
      { icon: Landmark, label: "Payments", to: "/payments" },
      { icon: ReceiptText, label: "Evidence", to: "/evidence" },
    ],
  },
  {
    label: "Operations",
    items: [
      { icon: Activity, label: "Health", to: "/health" },
      { icon: CreditCard, label: "Billing", to: "/billing" },
    ],
  },
  {
    label: "Developer / Admin",
    items: [
      { icon: ClipboardList, label: "Demo Script", to: "/demo-script" },
      { icon: ShieldCheck, label: "Auth Debug", to: "/auth-debug" },
    ],
  },
];

export function Sidebar() {
  return (
    <aside className="sidebar" aria-label="Console navigation">
      <div className="sidebar-brand">
        <span className="brand-mark">
          <BarChart3 aria-hidden="true" size={22} />
        </span>
        <div>
          <strong>NkeanyiAI TrustFlow</strong>
          <span>Regulated AI Workflow Control</span>
        </div>
      </div>
      <span className="environment-badge">Local Demo / MVP</span>
      <nav className="sidebar-nav">
        {navSections.map((section) => (
          <div className="nav-section" key={section.label}>
            <span className="nav-section-label">{section.label}</span>
            {section.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}
              >
                <item.icon aria-hidden="true" size={17} strokeWidth={2.1} />
                {item.label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>
    </aside>
  );
}
