import { createFileRoute, Link, useNavigate, notFound } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { formatETB, type OfficerDecision } from "@/lib/mock-data";
import { useState } from "react";
import { ArrowLeft, AlertTriangle, CheckCircle2, FileEdit, ShieldAlert, Loader2, Check } from "lucide-react";
import { toast } from "sonner";

export const Route = createFileRoute("/officer/case/$id")({
  component: CaseDetail,
});

function CaseDetail() {
  const { id } = Route.useParams();
  const c = useApp((s) => s.cases.find((x) => x.id === id));
  const submitOfficerDecision = useApp((s) => s.submitOfficerDecision);
  const navigate = useNavigate();

  const [decision, setDecision] = useState<OfficerDecision | null>(null);
  const [notes, setNotes] = useState("Reviewed evidence payload, schedule line items, and post-ledger validation results. Resolution notes are ready for the review item record.");
  const [externalCaseId, setExternalCaseId] = useState("");
  const [confirming, setConfirming] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState<{ decision: OfficerDecision; externalCaseId?: string; caseId: string; returnId: string } | null>(null);
  const [progress, setProgress] = useState<string[]>([]);

  if (done) {
    return <DoneScreen done={done} caseId={done.caseId} returnId={done.returnId} navigate={navigate} />;
  }

  if (!c) throw notFound();

  const submit = async () => {
    if (!decision) return;
    if (decision === "CONFIRM_FRAUD" && !externalCaseId.trim()) return;
    setConfirming(false);
    setSubmitting(true);
    const steps: string[] = [];
    const push = (s: string) => { steps.push(s); setProgress([...steps]); };

    push("Submitting decision...");
    await wait(500);
    push("Decision recorded");

    try {
      const res = await fetch(`http://localhost:8081/officer-review-items/${c.id}/decision`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'X-Actor-Id': 'officer-abebe-001'
        },
        body: JSON.stringify({
          decision,
          officerActorId: 'officer-abebe-001',
          narrative: notes,
          externalCaseId: decision === "CONFIRM_FRAUD" ? externalCaseId.trim() : undefined
        })
      });
      if (!res.ok) throw new Error('API Error');
      const data = await res.json();
      push("Server confirmed: " + (data.message || "Decision accepted"));
    } catch (e) {
      push("Warning: Could not connect to backend server, proceeding locally");
    }

    submitOfficerDecision(
      c.id,
      decision,
      notes,
      decision === "CONFIRM_FRAUD" ? externalCaseId.trim() : undefined,
    );
    await wait(400);

    if (decision === "CLEAR") {
      push("All review items resolved; parent return → COMPLETED");
      await wait(400);
      push("Publishing completion event");
      await wait(300);
      push("Done");
      setDone({ decision, caseId: c.id, returnId: c.returnId });
    } else {
      push("Fraud confirmed; parent return → FRAUD_CONFIRMED");
      await wait(400);
      push("External case created");
      await wait(300);
      push("Done");
      setDone({ decision, externalCaseId: externalCaseId.trim(), caseId: c.id, returnId: c.returnId });
    }

    toast.success(`Decision ${decision} submitted for ${c.id}`);
    setSubmitting(false);
  };

  return (
    <div className="space-y-6">
      <Link to="/officer/queue" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="h-4 w-4" /> Review Queue
      </Link>

      <div className="bg-card border border-border border-l-4 border-l-destructive rounded-lg p-5">
        <div className="flex items-start justify-between flex-wrap gap-3">
          <div>
            <div className="flex items-center gap-3">
              <span className="bg-destructive text-destructive-foreground text-[10px] uppercase font-bold px-2 py-0.5 rounded">{c.severity}</span>
              <span className="font-mono text-sm font-semibold">{c.id}</span>
              <span className="mono text-[10px] uppercase text-muted-foreground">{c.reviewType}</span>
            </div>
            <h1 className="text-xl font-semibold mt-2">{c.party}</h1>
            <div className="text-sm text-muted-foreground mt-1">
              <span className="mono">{c.tin}</span> · {c.taxType} {c.period} · Workflow: <span className="mono">{c.workflowInstanceId}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          {/* Risk */}
          <div className="bg-card border border-border rounded-lg p-5">
            <div className="flex items-center justify-between">
              <h2 className="font-semibold">Risk Engine Report</h2>
              <span className="text-xs text-muted-foreground">Computed: {c.createdAt}</span>
            </div>
            <div className="mt-3 flex items-center gap-4">
              <div className="bg-destructive/10 text-destructive px-3 py-1.5 rounded font-semibold text-sm">{c.severity}</div>
              <div className="text-sm">Score: <span className="mono font-semibold">{c.evidencePayload.riskScore?.toFixed(2) ?? "—"} / 1.00</span></div>
            </div>
            <div className="mt-4 space-y-2">
              {c.evidencePayload.indicators.map((ind, i) => (
                <div key={i} className="flex items-start gap-2 text-sm bg-muted/30 border border-border rounded-md p-2.5">
                  <AlertTriangle className="h-4 w-4 text-warning shrink-0 mt-0.5" />
                  <div><span className="mono text-xs text-muted-foreground">INDICATOR-{i + 1}:</span> {ind}</div>
                </div>
              ))}
            </div>
            <div className="mt-4 text-sm text-muted-foreground italic border-l-2 border-warning pl-3">
              "Multi-indicator {c.severity} risk: {c.evidencePayload.indicators.slice(0, 2).join(" combined with ")}."
            </div>
          </div>

          {/* Rule */}
          <div className="bg-card border border-border rounded-lg p-5">
            <h2 className="font-semibold">Rule Engine Report</h2>
            <div className="mt-3 flex items-center gap-3">
              <CheckCircle2 className="h-5 w-5 text-success" />
              <span className="font-semibold text-success">{c.evidencePayload.ruleResults?.[0] ?? "PASSED"}</span>
              <span className="text-xs text-muted-foreground">PostLedgerValidationResponse review evidence</span>
            </div>
          </div>

          {/* Summary */}
          <div className="bg-card border border-border rounded-lg p-5">
            <h2 className="font-semibold mb-3">Tax Return Summary</h2>
            <div className="text-xs text-muted-foreground mb-3">Period: {c.period} · Method: PORTAL · Rule Pkg: VAT-v2.1</div>
            <table className="w-full text-sm border border-border rounded-md overflow-hidden">
              <thead className="bg-muted/50 text-xs text-muted-foreground">
                <tr><th className="text-left px-3 py-2 font-medium">Schedule</th><th className="text-right px-3 py-2 font-medium">Amount</th><th className="text-right px-3 py-2 font-medium">Items</th><th className="text-right px-3 py-2 font-medium">Status</th></tr>
              </thead>
              <tbody>
                <tr className="border-t border-border"><td className="px-3 py-2 mono text-xs">SALES_OUTPUT</td><td className="px-3 py-2 text-right mono">ETB 630,000</td><td className="px-3 py-2 text-right">3</td><td className="px-3 py-2 text-right text-success">OK</td></tr>
                <tr className="border-t border-border"><td className="px-3 py-2 mono text-xs">PURCHASES_INPUT</td><td className="px-3 py-2 text-right mono">ETB 235,500</td><td className="px-3 py-2 text-right">2</td><td className="px-3 py-2 text-right text-success">OK</td></tr>
              </tbody>
            </table>
            <div className="mt-4 grid grid-cols-3 gap-4 text-sm">
              <div><div className="text-xs text-muted-foreground">Gross Tax</div><div className="mono">ETB 94,500</div></div>
              <div><div className="text-xs text-muted-foreground">Input Credit</div><div className="mono text-destructive">(ETB 33,075)</div></div>
              <div><div className="text-xs text-muted-foreground">Net Tax</div><div className="mono font-semibold">{formatETB(c.netTax)}</div></div>
            </div>
            <div className="mt-3 text-xs text-muted-foreground">Ledger Entry: <span className="mono text-foreground">LED-2026-00483</span> · Posted: {c.createdAt}</div>
          </div>
        </div>

        {/* Decision Panel */}
        <aside className="lg:sticky lg:top-4 self-start">
          <div className="bg-card border border-border rounded-lg p-5">
            <h2 className="font-semibold mb-4">Submit Decision</h2>
            <div className="text-xs text-muted-foreground mb-3">Officer ID: <span className="mono text-foreground">officer-abebe-001</span></div>

            <div className="space-y-2 mb-4">
              {([
                ["CLEAR", CheckCircle2, "text-success", "Mark reviewed; no issue found. Return → COMPLETED"],
                ["CONFIRM_FRAUD", ShieldAlert, "text-destructive", "Confirm fraud. Return → FRAUD_CONFIRMED (terminal)"],
              ] as const).map(([key, Icon, color, sub]) => (
                <label key={key} className={`flex items-start gap-3 p-3 border rounded-md cursor-pointer transition-colors ${decision === key ? "border-accent bg-accent/5" : "border-border hover:bg-muted/30"
                  }`}>
                  <input type="radio" name="dec" checked={decision === key} onChange={() => setDecision(key)} className="mt-1 accent-accent" />
                  <Icon className={`h-4 w-4 mt-0.5 ${color}`} />
                  <div>
                    <div className="text-sm font-medium">{key.replace(/_/g, " ")}</div>
                    <div className="text-xs text-muted-foreground">{sub}</div>
                  </div>
                </label>
              ))}
            </div>

            {decision === "CONFIRM_FRAUD" && (
              <div className="mb-4">
                <div className="text-xs font-medium text-muted-foreground mb-1.5">External Case ID <span className="text-destructive">*</span></div>
                <input
                  value={externalCaseId}
                  onChange={(e) => setExternalCaseId(e.target.value)}
                  placeholder="CASE-2026-118"
                  className="input mono"
                />
              </div>
            )}

            <div>
              <div className="text-xs font-medium text-muted-foreground mb-1.5">Narrative / Notes <span className="text-destructive">*</span></div>
              <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={6} className="input resize-none" />
            </div>

            <div className="mt-4 flex gap-2">
              <button className="btn-ghost flex-1" onClick={() => navigate({ to: "/officer/queue" })}>Cancel</button>
              <button
                disabled={!decision || !notes.trim() || (decision === "CONFIRM_FRAUD" && !externalCaseId.trim())}
                className="btn-primary flex-1 disabled:opacity-50"
                onClick={() => setConfirming(true)}
              >
                Submit Resolution
              </button>
            </div>
          </div>
        </aside>
      </div>

      {/* Confirm modal */}
      {confirming && decision && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4 fade-in">
          <div className="bg-card border border-border rounded-lg p-6 max-w-md w-full">
            <h3 className="font-semibold text-lg">Confirm Decision</h3>
            <div className="mt-4 text-sm space-y-2">
              <div><span className="text-muted-foreground w-24 inline-block">Resolution:</span><b>{decision.replace(/_/g, " ")}</b></div>
              <div><span className="text-muted-foreground w-24 inline-block">Review Item:</span><span className="mono">{c.id}</span></div>
              {decision === "CONFIRM_FRAUD" && <div><span className="text-muted-foreground w-24 inline-block">External Case:</span><span className="mono">{externalCaseId}</span></div>}
              <div><span className="text-muted-foreground w-24 inline-block">Officer:</span>Abebe Girma</div>
            </div>
            <div className="mt-4 bg-warning/10 border border-warning/30 rounded p-3 text-xs text-warning flex gap-2 items-start">
              <AlertTriangle className="h-4 w-4 shrink-0" /> This action cannot be undone.
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button className="btn-ghost" onClick={() => setConfirming(false)}>Go Back</button>
              <button className="btn-primary" onClick={submit}>Confirm &amp; Submit</button>
            </div>
          </div>
        </div>
      )}

      {submitting && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-lg p-6 max-w-sm w-full">
            <div className="flex items-center gap-2 mb-4"><Loader2 className="h-5 w-5 animate-spin text-accent" /><span className="font-semibold">Processing decision</span></div>
            <div className="space-y-2">
              {progress.map((p, i) => (
                <div key={i} className="flex items-center gap-2 text-sm fade-in">
                  <Check className="h-4 w-4 text-success" /> {p}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function DoneScreen({ done, caseId, returnId, navigate }: { done: { decision: OfficerDecision; externalCaseId?: string }; caseId: string; returnId: string; navigate: ReturnType<typeof useNavigate> }) {
  if (done.decision === "CLEAR") {
    return (
      <div className="max-w-2xl mx-auto bg-card border border-success/40 rounded-lg p-8 text-center fade-in">
        <CheckCircle2 className="h-12 w-12 text-success mx-auto mb-4" />
        <h2 className="text-xl font-semibold">Review Resolved — CLEAR</h2>
        <p className="mt-2 text-muted-foreground text-sm">Tax return <span className="mono">{returnId}</span> is now COMPLETED. Taxpayer notified automatically.</p>
        <div className="mt-6 text-sm text-muted-foreground">
          Decision By: Officer Abebe Girma
        </div>
        <button onClick={() => navigate({ to: "/officer/queue" })} className="mt-6 btn-primary mx-auto">← Back to Queue</button>
      </div>
    );
  }
  return (
    <div className="max-w-2xl mx-auto bg-card border border-destructive/40 rounded-lg p-8 fade-in">
      <h2 className="text-xl font-semibold flex items-center gap-2 text-destructive"><ShieldAlert className="h-6 w-6" /> Fraud Confirmed</h2>
      <p className="mt-2 text-sm text-muted-foreground">Tax return <span className="mono">{returnId}</span> is now FRAUD_CONFIRMED. This is a terminal state.</p>
      <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
        <div><div className="text-xs text-muted-foreground">External Case ID</div><div className="mono font-semibold">{done.externalCaseId}</div></div>
        <div><div className="text-xs text-muted-foreground">Tax Return</div><div className="font-semibold text-destructive">FRAUD_CONFIRMED</div></div>
        <div><div className="text-xs text-muted-foreground">Review Status</div><div>RESOLVED</div></div>
        <div><div className="text-xs text-muted-foreground">Officer</div><div>Abebe Girma</div></div>
      </div>
      <div className="mt-6 flex gap-2">
        <button onClick={() => navigate({ to: "/officer/queue" })} className="btn-ghost">← Back to Queue</button>
      </div>
    </div>
  );
}

function wait(ms: number) { return new Promise((r) => setTimeout(r, ms)); }
