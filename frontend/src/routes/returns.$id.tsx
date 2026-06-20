import { createFileRoute, Link, notFound, useNavigate } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { StatusBadge } from "@/components/StatusBadge";
import { formatETB } from "@/lib/mock-data";
import { useState } from "react";
import { ArrowLeft, AlertTriangle, Clock, RefreshCcw, ShieldAlert } from "lucide-react";

import { RouteError } from "@/components/RouteError";

export const Route = createFileRoute("/returns/$id")({
  component: ReturnDetail,
  errorComponent: RouteError,
});

const TABS = ["Overview", "Schedules & Line Items", "Calculation History", "Audit Trail"] as const;

function ReturnDetail() {
  const { id } = Route.useParams();
  const r = useApp((s) => s.returns.find((x) => x.id === id));
  const complianceChecks = useApp((s) => s.complianceChecks).filter((c) => c.returnId === id);
  const setStatus = useApp((s) => s.setStatus);
  const appendEvent = useApp((s) => s.appendEvent);
  const navigate = useNavigate();
  const [tab, setTab] = useState<typeof TABS[number]>("Overview");
  if (!r) throw notFound();

  return (
    <div className="space-y-6">
      <Link to="/returns" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-4 w-4" /> Back to returns
      </Link>

      {r.status === "MANUAL_REVIEW" && (
        <div className="bg-warning/10 border border-warning/40 rounded-md p-4 flex gap-3">
          <AlertTriangle className="h-5 w-5 text-warning shrink-0 mt-0.5" />
          <div className="text-sm">
            <div className="font-semibold text-warning">This return has been flagged for officer review.</div>
            <div className="text-muted-foreground mt-1">A tax officer will contact you within 3 business days. Reference: <span className="mono">{r.reviewId}</span></div>
          </div>
        </div>
      )}
      {r.status === "AMENDMENT_DRAFT" && (
        <div className="bg-purple-500/10 border border-purple-500/40 rounded-md p-4 flex gap-3 items-center">
          <div className="text-sm flex-1">
            <div className="font-semibold text-purple-700">Amendment Draft In Progress</div>
            <div className="text-muted-foreground mt-1">This return is in amendment draft. Correct the values and resubmit.</div>
          </div>
        </div>
      )}
      {r.status === "FRAUD_CONFIRMED" && (
        <div className="bg-destructive/10 border border-destructive/40 rounded-md p-4 flex gap-3">
          <ShieldAlert className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
          <div className="text-sm">
            <div className="font-semibold text-destructive">Fraud Confirmed — Terminal State</div>
            <div className="text-muted-foreground mt-1">This return has been flagged as fraudulent. External case ID: <span className="mono">{r.externalCaseId ?? "—"}</span>. Contact the revenue authority for further information.</div>
          </div>
        </div>
      )}
      {r.status === "CALCULATION_FAILED" && (
        <div className="bg-destructive/10 border border-destructive/40 rounded-md p-4 flex gap-3">
          <AlertTriangle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
          <div className="text-sm">
            <div className="font-semibold text-destructive">Calculation failed.</div>
            <div className="text-muted-foreground mt-1">The backend can retry calculation up to 10 iterations after line items or rule package cache issues are corrected.</div>
          </div>
        </div>
      )}

      <div className="bg-card border border-border rounded-lg p-6">
        <div className="flex items-start justify-between flex-wrap gap-4">
          <div>
            <div className="text-xs uppercase text-muted-foreground tracking-wide">Tax Return</div>
            <h1 className="text-xl font-semibold mt-1 mono">{r.id}</h1>
            <div className="text-sm text-muted-foreground mt-1 mono">{r.tin} · {r.party} · {r.taxType} {r.period}</div>
          </div>
          <StatusBadge status={r.status} />
        </div>

        <Lifecycle status={r.status} />
      </div>

      <div className="border-b border-border flex gap-1">
        {TABS.map((t) => (
          <button key={t} onClick={() => setTab(t)} className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
            tab === t ? "border-accent text-accent" : "border-transparent text-muted-foreground hover:text-foreground"
          }`}>{t}</button>
        ))}
      </div>

      {tab === "Overview" && <OverviewTab r={r} complianceChecks={complianceChecks} />}
      {tab === "Schedules & Line Items" && <SchedulesTab r={r} />}
      {tab === "Calculation History" && <CalcTab r={r} />}
      {tab === "Audit Trail" && <AuditTab r={r} />}
    </div>
  );
}

const PRIMARY_STAGES = ["DRAFT","CALCULATING","ACCEPTED","POSTED_TO_LEDGER","UNDER_VALIDATION","COMPLETED"] as const;
const AMENDMENT_STAGES = ["AMENDMENT_DRAFT","AMENDMENT_CALCULATING","AMENDMENT_ACCEPTED","AMENDMENT_POSTED","UNDER_VALIDATION","COMPLETED"] as const;

function Lifecycle({ status }: { status: string }) {
  if (status === "FRAUD_CONFIRMED") {
    return (
      <div className="mt-6">
        <div className="flex items-center">
          {["DRAFT", "CALCULATING", "ACCEPTED", "POSTED_TO_LEDGER", "UNDER_VALIDATION", "MANUAL_REVIEW", "FRAUD_CONFIRMED"].map((s, i) => (
            <div key={s} className="flex items-center flex-1 last:flex-none">
              <div className={`h-3 w-3 rounded-full shrink-0 ${s === "FRAUD_CONFIRMED" ? "bg-destructive" : "bg-accent"}`} />
              <div className="ml-2 text-[10px] uppercase tracking-wide font-medium hidden md:block whitespace-nowrap">
                <span className={s === "FRAUD_CONFIRMED" ? "text-destructive" : "text-foreground"}>{s.replace(/_/g, " ")}</span>
              </div>
              {i < 6 && <div className={`flex-1 h-px mx-2 ${s === "FRAUD_CONFIRMED" ? "bg-destructive" : "bg-accent"}`} />}
            </div>
          ))}
        </div>
        <div className="mt-3 text-xs text-destructive inline-flex items-center gap-1">
          <ShieldAlert className="h-3.5 w-3.5" /> Terminal state — no further transitions allowed.
        </div>
      </div>
    );
  }

  if (status === "CALCULATION_FAILED") {
    return (
      <div className="mt-6">
        <div className="flex items-center">
          {["DRAFT", "CALCULATING", "CALCULATION_FAILED"].map((s, i) => (
            <div key={s} className="flex items-center flex-1 last:flex-none">
              <div className={`h-3 w-3 rounded-full shrink-0 ${i < 2 ? "bg-accent" : "bg-destructive"}`} />
              <div className="ml-2 text-[10px] uppercase tracking-wide font-medium hidden md:block whitespace-nowrap">
                <span className={i < 2 ? "text-foreground" : "text-destructive"}>{s.replace(/_/g, " ")}</span>
              </div>
              {i < 2 && <div className="flex-1 h-px mx-2 bg-accent" />}
            </div>
          ))}
        </div>
        <div className="mt-3 text-xs text-destructive inline-flex items-center gap-1">
          <RefreshCcw className="h-3.5 w-3.5" /> Dead-end branch until a retry succeeds.
        </div>
      </div>
    );
  }

  const stages = status.startsWith("AMENDMENT") ? AMENDMENT_STAGES : PRIMARY_STAGES;
  let idx = stages.indexOf(status as never);
  if (status === "MANUAL_REVIEW") idx = Math.max(0, PRIMARY_STAGES.indexOf("UNDER_VALIDATION"));
  if (idx < 0) idx = 0;

  return (
    <div className="mt-6 flex items-center">
      {stages.map((s, i) => (
        <div key={s} className="flex items-center flex-1 last:flex-none">
          <div className={`h-3 w-3 rounded-full shrink-0 ${i <= idx ? "bg-accent" : "bg-muted border border-border"}`} />
          <div className="ml-2 text-[10px] uppercase tracking-wide font-medium hidden md:block whitespace-nowrap">
            <span className={i <= idx ? "text-foreground" : "text-muted-foreground"}>{s.replace(/_/g, " ")}</span>
          </div>
          {i < stages.length - 1 && <div className={`flex-1 h-px mx-2 ${i < idx ? "bg-accent" : "bg-border"}`} />}
        </div>
      ))}
    </div>
  );
}

function OverviewTab({ r, complianceChecks }: { r: ReturnType<typeof useApp.getState>["returns"][number]; complianceChecks: import("@/lib/mock-data").ComplianceCheck[] }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Stat label="Gross Tax" value={formatETB(r.grossTax)} />
      <Stat label="VAT Credit" value={formatETB(r.inputCredit)} />
      <Stat label="Net Payable" value={formatETB(r.netTax)} highlight />
      <div className="md:col-span-3 bg-card border border-border rounded-lg p-5 grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
        <Info label="Ledger Entry" value={r.ledgerEntry || "—"} mono />
        <Info label="Risk Result" value={r.riskLevel ? `${r.riskLevel} — ${r.riskScore?.toFixed(2)}` : "—"} tone={r.riskLevel === "HIGH" ? "danger" : r.riskLevel === "LOW" ? "success" : undefined} />
        <Info label="Rule Result" value={r.rulePassed === undefined ? "—" : r.rulePassed ? "PASSED" : "FAILED"} tone={r.rulePassed ? "success" : r.rulePassed === false ? "danger" : undefined} />
      </div>
      {complianceChecks.length > 0 && (
        <div className="md:col-span-3 bg-card border border-border rounded-lg p-5">
          <h3 className="font-semibold text-sm mb-3">Compliance Checks</h3>
          <div className="space-y-2">
            {complianceChecks.map((cc) => (
              <div key={cc.id} className="flex items-center gap-3 text-sm border border-border rounded-md p-3">
                <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded ${
                  cc.riskLevel === "HIGH" ? "bg-destructive/15 text-destructive" : cc.riskLevel === "MEDIUM" ? "bg-warning/15 text-warning" : "bg-success/15 text-success"
                }`}>{cc.riskLevel}</span>
                <span className="mono text-xs text-muted-foreground">{cc.id}</span>
                <span>Score: <span className="mono font-semibold">{cc.riskScore.toFixed(2)}</span></span>
                <span className={cc.rulePassed ? "text-success" : "text-destructive"}>Rule: {cc.rulePassed ? "PASSED" : "FAILED"}</span>
                <span className="ml-auto text-xs text-muted-foreground">{cc.outcome}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
function Stat({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`bg-card border rounded-lg p-5 ${highlight ? "border-accent" : "border-border"}`}>
      <div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className={`mt-2 text-xl font-semibold mono ${highlight ? "text-accent" : ""}`}>{value}</div>
    </div>
  );
}
function Info({ label, value, mono, tone }: { label: string; value: string; mono?: boolean; tone?: "success" | "danger" }) {
  const t = tone === "success" ? "text-success" : tone === "danger" ? "text-destructive" : "text-foreground";
  return (
    <div>
      <div className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className={`mt-1 ${mono ? "mono" : ""} ${t}`}>{value}</div>
    </div>
  );
}

function SchedulesTab({ r }: { r: ReturnType<typeof useApp.getState>["returns"][number] }) {
  if (r.schedules.length === 0) return <Empty text="No schedules recorded for this return." />;
  return (
    <div className="space-y-4">
      {r.schedules.map((sch) => (
        <div key={sch.scheduleCode} className="bg-card border border-border rounded-lg overflow-hidden">
          <div className="px-5 py-3 bg-muted/40 border-b border-border flex justify-between items-center">
            <div><span className="mono text-xs text-muted-foreground">{sch.scheduleCode}</span> <span className="ml-2 font-medium">{sch.label}</span></div>
            <div className="text-xs text-muted-foreground">{sch.items.length} items</div>
          </div>
          <table className="w-full text-sm">
            <thead className="text-xs text-muted-foreground">
              <tr>
                <th className="text-left px-5 py-2 font-medium">Order</th>
                <th className="text-left px-5 py-2 font-medium">Line Code</th>
                <th className="text-left px-5 py-2 font-medium">Text Value</th>
                <th className="text-right px-5 py-2 font-medium">Amount</th>
                <th className="text-left px-5 py-2 font-medium">Origin</th>
                <th className="text-right px-5 py-2 font-medium">Iteration</th>
              </tr>
            </thead>
            <tbody>
              {sch.items.map((it) => (
                <tr key={it.id} className="border-t border-border">
                  <td className="px-5 py-2 text-muted-foreground mono text-xs">{it.displayOrder}</td>
                  <td className="px-5 py-2 mono text-xs">{it.lineCode}</td>
                  <td className="px-5 py-2">{it.lineTextValue ?? "—"}</td>
                  <td className="px-5 py-2 text-right mono">{formatETB(it.amount)}</td>
                  <td className="px-5 py-2"><OriginBadge origin={it.origin} /></td>
                  <td className="px-5 py-2 text-right mono text-xs">{it.calculatedInIterationNo ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
    </div>
  );
}

function CalcTab({ r }: { r: ReturnType<typeof useApp.getState>["returns"][number] }) {
  if (!r.calculationIterations || r.calculationIterations.length === 0) return <Empty text="No calculation iterations yet." />;
  return (
    <div className="space-y-3">
      <div className="bg-muted/40 border border-border rounded-md px-4 py-3 text-sm flex items-center justify-between">
        <span>Calculation iteration cap</span>
        <span className="mono font-semibold">{r.calculationIterations.length} / 10</span>
      </div>
      {r.calculationIterations.map((iteration) => (
        <div key={iteration.iterationNo} className="bg-card border border-border rounded-lg p-5 space-y-3">
          <div className="flex items-center justify-between text-sm">
            <div className="font-medium">
              ITER-{String(iteration.iterationNo).padStart(3, "0")}{" "}
              <span className={`ml-2 text-xs ${iteration.status === "COMPLETED" ? "text-success" : "text-destructive"}`}>
                {iteration.status === "COMPLETED" ? "accepted" : "failed"}
              </span>
            </div>
            <div className="text-xs text-muted-foreground mono">{iteration.durationMs}ms</div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs mono">
            <Hash label="Input Hash" value={iteration.inputHash} />
            <Hash label="Output Hash" value={iteration.outputHash ?? "—"} />
          </div>
          {iteration.errorPayload && (
            <div className="bg-destructive/10 border border-destructive/30 rounded p-3 text-xs text-destructive">{iteration.errorPayload}</div>
          )}
          {iteration.status === "COMPLETED" && (
            <div className="text-sm grid grid-cols-3 gap-4 mono">
              <div>Output: {formatETB(r.grossTax)}</div>
              <div>Credit: {formatETB(r.inputCredit)}</div>
              <div>Net: {formatETB(r.netTax)}</div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function Hash({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className="mt-1 break-all">{value}</div>
    </div>
  );
}

function OriginBadge({ origin }: { origin: "USER_ENTERED" | "ENGINE_COMPUTED" | "OFFICER_OVERRIDE" }) {
  const style = origin === "USER_ENTERED"
    ? "bg-success/15 text-success"
    : origin === "ENGINE_COMPUTED"
    ? "bg-info/15 text-info"
    : "bg-warning/15 text-warning";
  return (
    <span className={`text-[10px] uppercase font-semibold px-2 py-0.5 rounded ${style}`}>
      {origin.replace(/_/g, " ")}
    </span>
  );
}

function AuditTab({ r }: { r: ReturnType<typeof useApp.getState>["returns"][number] }) {
  if (r.events.length === 0) return <Empty text="No audit events yet." />;
  return (
    <div className="bg-card border border-border rounded-lg p-5">
      <ol className="relative border-l border-border ml-3">
        {r.events.map((ev, i) => (
          <li key={i} className="ml-5 pb-4 last:pb-0 relative">
            <div className="absolute -left-7 top-1.5 h-2.5 w-2.5 rounded-full bg-accent" />
            <div className="flex items-baseline gap-3">
              <span className="mono text-xs text-muted-foreground w-12">{ev.time}</span>
              <span className="text-sm font-medium">{ev.label}</span>
              <span className={`text-[10px] uppercase tracking-wide font-semibold px-1.5 py-0.5 rounded ${
                ev.actor === "SYSTEM" ? "bg-accent/15 text-accent" : ev.actor === "TAXPAYER" ? "bg-success/15 text-success" : "bg-muted text-muted-foreground"
              }`}>{ev.actor}</span>
              {ev.detail && <span className="text-xs text-muted-foreground mono">{ev.detail}</span>}
            </div>
          </li>
        ))}
      </ol>
    </div>
  );
}
function Empty({ text }: { text: string }) {
  return <div className="text-sm text-muted-foreground bg-muted/30 border border-dashed border-border rounded-md p-8 text-center"><Clock className="inline h-4 w-4 mr-2" />{text}</div>;
}

function timeNow() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}
