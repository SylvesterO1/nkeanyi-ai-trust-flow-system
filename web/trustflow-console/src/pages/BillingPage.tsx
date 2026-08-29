import { MetricCard, PageHeader, ProductCard } from "../components/ProductUI";

const usageCards = [
  ["Workflow Runs", "1,248", "Commercial roadmap", "Controlled workflow executions that can become billable units."],
  ["Evidence Events", "2,604", "Commercial roadmap", "Audit evidence records available for metering and retention tiers."],
  ["Tenant Usage", "3 tenants", "Commercial roadmap", "Tenant-level allocation for enterprise packaging."],
  ["API Consumption", "5 services", "Commercial roadmap", "Gateway-mediated service usage for future usage plans."],
];

const packageSignals = [
  ["Metered events", "Documents, grounded answers, payment workflows, evidence records, and audit exports."],
  ["Tenant allocation", "Usage can be grouped by enterprise tenant or business unit."],
  ["Audit retention", "Evidence storage duration can support tiered commercial packaging."],
  ["Governance exports", "Compliance-ready exports can become enterprise add-ons."],
];

const roadmapRows = [
  ["Usage collection", "Planned", "Connect real workflow records to usage counters."],
  ["Plan management", "Planned", "Package tenant count, retention, and workflow volume."],
  ["Invoice generation", "Planned", "Generate customer-facing invoices from metered activity."],
  ["Enterprise controls", "Planned", "Add contract terms, retention policy, and admin approvals."],
];

export function BillingPage() {
  return (
    <section className="page-stack">
      <PageHeader
        title="Billing & Usage"
        subtitle="Usage metering and enterprise billing controls are planned for commercial packaging."
      >
        <span className="demo-data-badge">Coming soon / commercial roadmap</span>
      </PageHeader>

      <div className="stats-grid billing-stats-grid">
        {usageCards.map(([label, value, badge, helperText]) => (
          <MetricCard key={label} label={label} value={value} badge={badge} helperText={helperText} />
        ))}
      </div>

      <ProductCard title="Commercial Packaging Direction" subtitle="How NAITFS usage can map to enterprise pricing.">
        <div className="summary-grid billing-plan-grid">
          {packageSignals.map(([label, value]) => (
            <div key={label}>
              <span>{label}</span>
              <strong>{value}</strong>
            </div>
          ))}
        </div>
      </ProductCard>

      <ProductCard title="Billing Roadmap" subtitle="Frontend placeholder until backend metering and invoicing are implemented.">
        <div className="table-wrap billing-table">
          <table>
            <thead>
              <tr>
                <th>Capability</th>
                <th>Status</th>
                <th>Commercial purpose</th>
              </tr>
            </thead>
            <tbody>
              {roadmapRows.map(([capability, status, purpose]) => (
                <tr key={capability}>
                  <td>{capability}</td>
                  <td>
                    <span className="demo-data-badge">{status}</span>
                  </td>
                  <td>{purpose}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </ProductCard>

      <ProductCard title="Commercialization Note" subtitle="No backend billing behavior changed in MVP18.2.">
        <p className="commercial-summary">
          Billing remains a commercial packaging view. The product story is that billable usage should be
          derived from traceable workflow events, tenant activity, evidence capture, and governed audit exports,
          not arbitrary page views or opaque AI calls.
        </p>
      </ProductCard>
    </section>
  );
}
