import { createFileRoute, Link } from "@tanstack/react-router";
import { useApp } from "@/lib/store";
import { Clock } from "lucide-react";

export const Route = createFileRoute("/officer/")({
  component: OfficerDashboard,
});

function OfficerDashboard() {
  const cases = useApp((s) => s.cases);
  const decisions = useApp((s) => s.decisions);
  const counts = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 };
  cases.forEach((c) => counts[c.severity]++);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Officer Dashboard</h1>
        <p className="text-sm text-muted-foreground mt-1">Officer Abebe Girma · officer-001</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card label="Open Reviews" value={String(cases.length)} tone="warning" />
        <Card label="CRITICAL Severity" value={String(counts.CRITICAL)} tone="danger" />
        <Card label="Reviewed Today" value={String(decisions.length + 3)} tone="success" />
        <Card label="Avg Resolution" value="4.2 hrs" tone="info" />
      </div>

      {counts.CRITICAL > 0 && (
        <div className="bg-destructive/10 border border-destructive/40 rounded-md p-4 flex items-center gap-3">
          <Clock className="h-5 w-5 text-destructive" />
          <div className="text-sm text-destructive">
            <b>{counts.CRITICAL} case</b> approaching SLA breach in &lt; 2 hours — review now.
          </div>
          <Link to="/officer/queue" className="ml-auto btn-primary">Open Queue</Link>
        </div>
      )}

      <div className="bg-card border border-border rounded-lg p-6">
        <h2 className="font-semibold mb-4">Severity Queue Summary</h2>
        <div className="space-y-3">
          {(["CRITICAL", "HIGH", "MEDIUM", "LOW"] as const).map((p) => {
            const max = Math.max(1, ...Object.values(counts));
            const w = (counts[p] / max) * 100;
            const colors = { CRITICAL: "bg-destructive", HIGH: "bg-warning", MEDIUM: "bg-info", LOW: "bg-muted-foreground" };
            return (
              <div key={p} className="flex items-center gap-3">
                <div className="w-20 text-xs font-semibold">{p}</div>
                <div className="flex-1 h-3 bg-muted rounded-full overflow-hidden">
                  <div className={`h-full ${colors[p]} transition-all`} style={{ width: `${w}%` }} />
                </div>
                <div className="w-8 text-right mono text-sm">{counts[p]}</div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function Card({ label, value, tone }: { label: string; value: string; tone: "warning" | "danger" | "success" | "info" }) {
  const t = { warning: "text-warning", danger: "text-destructive", success: "text-success", info: "text-accent" }[tone];
  return (
    <div className="bg-card border border-border rounded-lg p-5">
      <div className="text-xs uppercase tracking-wide text-muted-foreground font-medium">{label}</div>
      <div className={`mt-2 text-2xl font-semibold ${t}`}>{value}</div>
    </div>
  );
}
