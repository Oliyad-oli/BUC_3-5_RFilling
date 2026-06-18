import { createFileRoute } from "@tanstack/react-router";
import { useApp } from "@/lib/store";

export const Route = createFileRoute("/system/validation")({ component: Validation });

function Validation() {
  const checks = useApp((s) => s.complianceChecks);

  const highCount = checks.filter((c) => c.riskLevel === "HIGH").length;
  const mediumCount = checks.filter((c) => c.riskLevel === "MEDIUM").length;
  const lowCount = checks.filter((c) => c.riskLevel === "LOW").length;
  const maxRisk = Math.max(highCount, mediumCount, lowCount, 1);
  const risk = [
    { k: "HIGH", v: highCount, c: "bg-destructive" },
    { k: "MEDIUM", v: mediumCount, c: "bg-warning" },
    { k: "LOW", v: lowCount, c: "bg-success" },
  ];

  const passedCount = checks.filter((c) => c.rulePassed).length;
  const failedCount = checks.filter((c) => !c.rulePassed).length;
  const totalRules = Math.max(passedCount + failedCount, 1);

  // Aggregate top risk indicators across all checks
  const indicatorCounts: Record<string, number> = {};
  for (const c of checks) {
    for (const ind of c.riskIndicators) {
      indicatorCounts[ind] = (indicatorCounts[ind] ?? 0) + 1;
    }
  }
  const topIndicators = Object.entries(indicatorCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  // Recent checks as events
  const recentChecks = checks.slice(0, 8);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Validation Engine</h1>
        <p className="text-sm text-muted-foreground mt-1">{checks.length} compliance checks on record</p>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-card border border-border rounded-lg p-5">
          <h2 className="font-semibold mb-4">Risk Engine Results</h2>
          <div className="space-y-2">
            {risk.map((r) => (
              <div key={r.k} className="flex items-center gap-3">
                <div className="w-20 text-xs font-semibold">{r.k}</div>
                <div className="flex-1 h-3 bg-muted rounded-full overflow-hidden"><div className={`h-full ${r.c}`} style={{ width: `${(r.v / maxRisk) * 100}%` }} /></div>
                <div className="w-8 text-right mono text-sm">{r.v}</div>
              </div>
            ))}
          </div>
          <h3 className="font-semibold mt-6 mb-3 text-sm">Top Risk Indicators</h3>
          <div className="space-y-1.5">
            {topIndicators.length === 0 && (
              <div className="text-sm text-muted-foreground">No risk indicators recorded yet.</div>
            )}
            {topIndicators.map(([label, count]) => (
              <div key={label} className="flex justify-between text-sm border-b border-border pb-1.5">
                <span>{label}</span><span className="mono text-muted-foreground">{count} case{count !== 1 ? "s" : ""}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="bg-card border border-border rounded-lg p-5">
          <h2 className="font-semibold mb-4">Rule Engine Results</h2>
          <div className="space-y-2">
            <div className="flex items-center gap-3"><div className="w-20 text-xs font-semibold">PASSED</div><div className="flex-1 h-3 bg-muted rounded-full overflow-hidden"><div className="h-full bg-success" style={{ width: `${(passedCount / totalRules) * 100}%` }} /></div><div className="w-8 text-right mono text-sm">{passedCount}</div></div>
            <div className="flex items-center gap-3"><div className="w-20 text-xs font-semibold">FAILED</div><div className="flex-1 h-3 bg-muted rounded-full overflow-hidden"><div className="h-full bg-destructive" style={{ width: `${(failedCount / totalRules) * 100}%` }} /></div><div className="w-8 text-right mono text-sm">{failedCount}</div></div>
          </div>
          <h3 className="font-semibold mt-6 mb-3 text-sm">Recent Validation Events</h3>
          <div className="space-y-1.5 mono text-xs">
            {recentChecks.length === 0 && (
              <div className="text-sm text-muted-foreground font-sans">No validation events yet. File a return and run validation to see results here.</div>
            )}
            {recentChecks.map((cc) => (
              <div key={cc.id} className="flex gap-3 items-center border-b border-border pb-1.5">
                <span className="text-muted-foreground">{cc.checkedAt}</span>
                <span className="font-semibold">{cc.returnId}</span>
                <span>Risk={cc.riskLevel}</span><span>Rule={cc.rulePassed ? "PASS" : "FAIL"}</span>
                <span className={`ml-auto ${cc.outcome === "COMPLETED" ? "text-success" : "text-warning"}`}>→ {cc.outcome}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
