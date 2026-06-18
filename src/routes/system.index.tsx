import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/system/")({ component: SystemOverview });

function SystemOverview() {
  const engines = [
    { name: "Risk Engine", latency: "48ms" },
    { name: "Rule Engine", latency: "31ms" },
    { name: "Ledger Engine", latency: "122ms" },
    { name: "Workflow", latency: "0 stuck" },
  ];
  const funnel = [
    { label: "Returns Filed Today", value: 47 },
    { label: "Accepted & Queued", value: 44 },
    { label: "Posted to Ledger", value: 42 },
    { label: "Validation Completed", value: 38 },
    { label: "Completed (auto)", value: 31 },
    { label: "Flagged for Review", value: 7 },
    { label: "Officer Queue (open)", value: 3 },
  ];
  const max = funnel[0].value;
  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-semibold">System Health</h1>
          <p className="text-sm text-muted-foreground mt-1">Real-time platform status</p>
        </div>
        <div className="inline-flex items-center gap-2 text-sm text-success"><span className="pulse-dot" /> All systems operational</div>
      </div>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {engines.map((e) => (
          <div key={e.name} className="bg-card border border-border rounded-lg p-5">
            <div className="text-xs text-muted-foreground uppercase tracking-wide">{e.name}</div>
            <div className="mt-2 text-success font-semibold">● Online</div>
            <div className="text-xs text-muted-foreground mt-1 mono">{e.latency} avg</div>
          </div>
        ))}
      </div>
      <div className="bg-card border border-border rounded-lg p-6">
        <h2 className="font-semibold mb-4">Pipeline Status</h2>
        <div className="space-y-2">
          {funnel.map((f, i) => (
            <div key={f.label} className="flex items-center gap-3">
              <div className="w-56 text-sm text-muted-foreground">{f.label}</div>
              <div className="flex-1 h-7 bg-muted rounded overflow-hidden relative">
                <div className="h-full bg-accent transition-all" style={{ width: `${(f.value / max) * 100}%`, opacity: 1 - i * 0.1 }} />
              </div>
              <div className="w-12 text-right mono text-sm font-semibold">{f.value}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
