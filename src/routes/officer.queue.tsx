import { createFileRoute, Link } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { formatETB, type Severity } from "@/lib/mock-data";
import { Clock } from "lucide-react";

export const Route = createFileRoute("/officer/queue")({
  component: OfficerQueue,
});

const borders: Record<Severity, string> = {
  CRITICAL: "border-l-destructive",
  HIGH: "border-l-warning",
  MEDIUM: "border-l-info",
  LOW: "border-l-muted-foreground",
};
const badges: Record<Severity, string> = {
  CRITICAL: "bg-destructive text-destructive-foreground",
  HIGH: "bg-warning text-warning-foreground",
  MEDIUM: "bg-info text-info-foreground",
  LOW: "bg-muted text-muted-foreground",
};

function OfficerQueue() {
  const allCases = useApp((s) => s.cases);
  const cases = allCases.filter((c) => c.status === "OPEN");
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Officer Review Queue</h1>
        <p className="text-sm text-muted-foreground mt-1">{cases.length} open case{cases.length !== 1 ? "s" : ""} · auto-refreshes every 30s</p>
      </div>

      <div className="space-y-3">
        {cases.length === 0 && (
          <div className="bg-card border border-dashed border-border rounded-lg p-12 text-center text-muted-foreground">
            <div className="text-base">All clear — no cases in your queue.</div>
          </div>
        )}
        {cases.map((c) => (
          <div key={c.id} className={`bg-card border border-border border-l-4 ${borders[c.severity]} rounded-md p-5 hover:shadow-sm transition-shadow`}>
            <div className="flex items-start justify-between gap-4 flex-wrap">
              <div className="space-y-1.5 flex-1 min-w-0">
                <div className="flex items-center gap-3 flex-wrap">
                  <span className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded ${badges[c.severity]}`}>{c.severity}</span>
                  <span className="font-mono text-sm font-semibold">{c.id}</span>
                  <span className="mono text-[10px] uppercase text-muted-foreground">{c.reviewType}</span>
                </div>
                <div className="text-sm">
                  <span className="mono text-xs text-muted-foreground">{c.tin}</span> · <span className="font-medium">{c.party}</span>
                </div>
                <div className="text-xs text-muted-foreground space-x-3">
                  <span>{c.taxType} {c.period}</span>
                  <span>· Net Tax: <span className="mono text-foreground">{formatETB(c.netTax)}</span></span>
                  <span>· Risk Score: <span className="text-destructive font-semibold">{c.evidencePayload.riskScore?.toFixed(2) ?? "—"}</span></span>
                </div>
                <div className="text-xs text-muted-foreground flex items-center gap-3">
                  <span>Created: {c.createdAt}</span>
                  <span className="inline-flex items-center gap-1 text-warning"><Clock className="h-3 w-3" /> Status: {c.status}</span>
                </div>
              </div>
              <Link to="/officer/case/$id" params={{ id: c.id }} className="btn-primary shrink-0">Review →</Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
