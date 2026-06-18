import { createFileRoute } from "@tanstack/react-router";
import { useApp } from "@/lib/store";

export const Route = createFileRoute("/officer/history")({ component: History });

const styles: Record<string, string> = {
  CLEAR: "bg-success/15 text-success",
  REQUEST_AMENDMENT: "bg-warning/15 text-warning",
  CONFIRM_FRAUD: "bg-destructive/15 text-destructive",
  // Legacy fallbacks
  NO_ACTION: "bg-success/15 text-success",
  AMENDMENT_REQUESTED: "bg-warning/15 text-warning",
  OFFICER_OVERRIDE_APPLIED: "bg-info/15 text-info",
  CASE_ESCALATED: "bg-destructive/15 text-destructive",
};

function History() {
  const decisions = useApp((s) => s.decisions);
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Decision History</h1>
        <p className="text-sm text-muted-foreground mt-1">All officer decisions on file</p>
      </div>
      <div className="bg-card border border-border rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground uppercase">
            <tr>
              <th className="px-5 py-3 text-left font-medium">Case ID</th>
              <th className="px-5 py-3 text-left font-medium">TIN</th>
              <th className="px-5 py-3 text-left font-medium">Company</th>
              <th className="px-5 py-3 text-left font-medium">Period</th>
              <th className="px-5 py-3 text-left font-medium">Decision</th>
              <th className="px-5 py-3 text-left font-medium">External Case ID</th>
              <th className="px-5 py-3 text-left font-medium">Officer</th>
              <th className="px-5 py-3 text-left font-medium">Decided</th>
            </tr>
          </thead>
          <tbody>
            {decisions.map((d) => (
              <tr key={d.id} className="border-t border-border hover:bg-muted/30">
                <td className="px-5 py-3 mono text-xs">{d.caseId}</td>
                <td className="px-5 py-3 mono text-xs">{d.tin}</td>
                <td className="px-5 py-3">{d.party}</td>
                <td className="px-5 py-3">{d.period}</td>
                <td className="px-5 py-3"><span className={`text-[10px] uppercase font-semibold px-2 py-0.5 rounded ${styles[d.decision] ?? "bg-muted text-muted-foreground"}`}>{d.decision.replace(/_/g, " ")}</span></td>
                <td className="px-5 py-3 mono text-xs">{d.externalCaseId ?? "—"}</td>
                <td className="px-5 py-3">{d.officer}</td>
                <td className="px-5 py-3 text-muted-foreground">{d.decidedAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
