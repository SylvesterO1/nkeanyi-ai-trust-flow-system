const state = {
  gatewayUrl: "http://localhost:8080",
  tenantId: "tenant-mvp13-demo"
};

const elements = {
  gatewayUrl: document.getElementById("gatewayUrl"),
  tenantId: document.getElementById("tenantId"),
  token: document.getElementById("token"),
  loadDashboard: document.getElementById("loadDashboard"),
  message: document.getElementById("message"),
  readinessBadge: document.getElementById("readinessBadge"),
  tenantValue: document.getElementById("tenantValue"),
  totalEvidence: document.getElementById("totalEvidence"),
  totalCorrelations: document.getElementById("totalCorrelations"),
  lastEventTime: document.getElementById("lastEventTime"),
  evidenceByType: document.getElementById("evidenceByType"),
  serviceParticipation: document.getElementById("serviceParticipation"),
  recentTrustFlows: document.getElementById("recentTrustFlows")
};

function normalizeGatewayUrl(value) {
  return value.trim().replace(/\/+$/, "");
}

function statusClass(status) {
  if (status === "READY" || status === "COMPLETE") {
    return "status-ready";
  }

  if (status === "INCOMPLETE" || status === "REVIEW_REQUIRED") {
    return "status-warn";
  }

  return "status-neutral";
}

function setMessage(text, isError = false) {
  elements.message.textContent = text;
  elements.message.classList.toggle("error", isError);
}

function formatDate(value) {
  if (!value) {
    return "No evidence";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

function renderReadiness(status) {
  const readiness = status === "NO_DEMO_DATA" ? "NO_EVIDENCE" : status || "NO_EVIDENCE";
  elements.readinessBadge.textContent = readiness;
  elements.readinessBadge.className = `status-badge ${statusClass(readiness)}`;
}

function renderMetrics(data) {
  elements.tenantValue.textContent = data.tenantId || state.tenantId;
  elements.totalEvidence.textContent = data.totals?.totalEvidence ?? 0;
  elements.totalCorrelations.textContent = data.totals?.totalCorrelations ?? 0;
  elements.lastEventTime.textContent = formatDate(data.totals?.latestEventTime);
}

function renderEvidenceByType(evidenceByType = {}) {
  const entries = Object.entries(evidenceByType).sort(([left], [right]) => left.localeCompare(right));

  if (entries.length === 0) {
    elements.evidenceByType.className = "evidence-list empty-state";
    elements.evidenceByType.textContent = "No evidence counts returned";
    return;
  }

  elements.evidenceByType.className = "evidence-list";
  elements.evidenceByType.replaceChildren(...entries.map(([type, count]) => {
    const row = document.createElement("div");
    row.className = "evidence-row";
    row.innerHTML = `<span>${escapeHtml(type)}</span><strong class="count-pill">${count}</strong>`;
    return row;
  }));
}

function collectSourceServices(trustFlows = []) {
  const services = new Map();

  trustFlows.forEach(flow => {
    (flow.sourceServices || []).forEach(service => {
      services.set(service, (services.get(service) || 0) + 1);
    });
  });

  return [...services.entries()].sort(([left], [right]) => left.localeCompare(right));
}

function renderServiceParticipation(trustFlows = []) {
  const services = collectSourceServices(trustFlows);

  if (services.length === 0) {
    elements.serviceParticipation.className = "service-list empty-state";
    elements.serviceParticipation.textContent = "No service participation returned";
    return;
  }

  elements.serviceParticipation.className = "service-list";
  elements.serviceParticipation.replaceChildren(...services.map(([service, count]) => {
    const row = document.createElement("div");
    row.className = "service-row";
    row.innerHTML = `<span>${escapeHtml(service)}</span><strong class="count-pill">${count}</strong>`;
    return row;
  }));
}

function renderTrustFlows(trustFlows = []) {
  if (trustFlows.length === 0) {
    elements.recentTrustFlows.innerHTML = '<tr><td colspan="5" class="empty-row">No trust flows returned for this tenant</td></tr>';
    return;
  }

  elements.recentTrustFlows.replaceChildren(...trustFlows.map(flow => {
    const status = flow.status || "NO_EVIDENCE";
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${escapeHtml(flow.correlationId || "N/A")}</td>
      <td><span class="flow-status ${statusClass(status)}">${escapeHtml(status)}</span></td>
      <td class="types-cell">${escapeHtml((flow.evidenceTypes || []).join(", ") || "None")}</td>
      <td class="services-cell">${escapeHtml((flow.sourceServices || []).join(", ") || "None")}</td>
      <td>${escapeHtml(formatDate(flow.lastEventTime))}</td>
    `;
    return row;
  }));
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

async function loadDashboard() {
  state.gatewayUrl = normalizeGatewayUrl(elements.gatewayUrl.value || state.gatewayUrl);
  state.tenantId = elements.tenantId.value.trim() || state.tenantId;
  elements.gatewayUrl.value = state.gatewayUrl;
  elements.tenantId.value = state.tenantId;

  const token = elements.token.value.trim();
  if (!token) {
    setMessage("Paste a bearer token with evidence.read scope before loading dashboard data.", true);
    return;
  }

  setMessage("Loading dashboard data...");
  elements.loadDashboard.disabled = true;

  try {
    const url = `${state.gatewayUrl}/api/v1/evidence/demo-dashboard?tenantId=${encodeURIComponent(state.tenantId)}`;
    const response = await fetch(url, {
      headers: {
        Authorization: `Bearer ${token}`,
        "X-Tenant-Id": state.tenantId,
        Accept: "application/json"
      }
    });

    const data = await response.json().catch(() => null);
    if (!response.ok) {
      const message = data?.message || `Gateway returned HTTP ${response.status}`;
      throw new Error(message);
    }

    renderReadiness(data.readinessStatus);
    renderMetrics(data);
    renderEvidenceByType(data.evidenceByType);
    renderServiceParticipation(data.recentTrustFlows);
    renderTrustFlows(data.recentTrustFlows);
    setMessage(`Dashboard loaded from ${state.gatewayUrl}`);
  } catch (error) {
    setMessage(error.message || "Unable to load dashboard data", true);
  } finally {
    elements.loadDashboard.disabled = false;
  }
}

elements.loadDashboard.addEventListener("click", loadDashboard);
elements.tenantId.addEventListener("input", () => {
  elements.tenantValue.textContent = elements.tenantId.value.trim() || state.tenantId;
});

renderReadiness("NO_EVIDENCE");
